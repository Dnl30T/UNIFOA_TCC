#!/usr/bin/env python3
"""
Seed form — creates the "Escala de Burnout" form in ScyllaDB, already ACTIVE
and assigned to Equipe Alpha (created_by counselor_carol).  All members of
Equipe Alpha except emp_eve receive random responses.  Matching
employee_results rows are inserted into PostgreSQL.

Requirements:
    pip install psycopg2-binary cassandra-driver

Usage:
    # Default connection settings (matches docker-compose defaults)
    python scripts/seed_form.py

    # Wipe existing form data before inserting
    python scripts/seed_form.py --reset

    # Custom connections
    python scripts/seed_form.py \\
        --db-url postgresql://unformulieren:unformulieren@localhost:5432/unformulieren \\
        --scylla-host localhost --scylla-port 9042 --scylla-keyspace psytrack
"""

import argparse
import json
import random
import sys
import uuid
from collections import namedtuple
from datetime import datetime, timezone
from pathlib import Path

import psycopg2
import psycopg2.extras

# ── Config ────────────────────────────────────────────────────────────────────

DEFAULT_DATA_FILE = Path(__file__).parent / "data" / "form_data.json"
DEFAULT_DB_URL = "postgresql://unformulieren:unformulieren@localhost:5432/unformulieren"
DEFAULT_SCYLLA_HOST = "localhost"
DEFAULT_SCYLLA_PORT = 9042
DEFAULT_SCYLLA_KEYSPACE = "psytrack"

COUNSELOR_USERNAME = "counselor_carol"
TEAM_NAME = "Equipe Alpha"
EXCLUDE_EMPLOYEE_USERNAME = "emp_eve"
FORM_DESCRIPTION = (
    "Avaliação de burnout ocupacional baseada em três dimensões: "
    "exaustão emocional, cinismo/distanciamento e eficácia profissional."
)

# Likert labels match form_data.json "response_scale" (index 0 → answer value 1)
LIKERT_OPTIONS = ["Nunca", "Raramente", "Às vezes", "Frequentemente", "Sempre"]
# 17 questions × 5.88 ≈ 99.96 — stays within the ≤ 100 validation limit
WEIGHT_PER_QUESTION = "5.88"

# Clinical flag assignments per section
SECTION_FLAGS: dict[str, list[str]] = {
    "exaustao_emocional":     ["flagFatigue", "flagSleep", "flagOverload"],
    "cinismo_distanciamento": ["flagDisengagement", "flagIsolation"],
    "eficacia_profissional":  ["flagStress"],
}

# ── Helpers ───────────────────────────────────────────────────────────────────


def load_form_data(path: Path) -> dict:
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def classify_risk(score: int) -> str:
    """Replicates EmployeeResultService.classifyBurnoutRisk."""
    if score >= 70:
        return "HIGH"
    if score >= 40:
        return "MEDIUM"
    return "LOW"


def calculate_helper_score(
    answers: dict,
    questions: list,
) -> int:
    """Replicates FormResponseService.calculateHelperScore (LIKERT variant).

    For LIKERT: min=1, max=count(option_* keys), answers are 1-indexed.
    Formula: (answer - min) / (max - min) * 100, weighted average.
    """
    q_by_id = {q["id"]: q for q in questions}
    weighted_total = 0.0
    total_weight = 0.0

    for q_id, value in answers.items():
        q = q_by_id.get(q_id)
        if q is None:
            continue
        weight = float(q["config"].get("weight", "0"))
        if weight <= 0:
            continue
        option_count = sum(1 for k in q["config"] if k.startswith("option_"))
        max_score = float(max(option_count, 1))
        min_score = 1.0  # LIKERT answers are 1-indexed
        score_range = max_score - min_score
        if score_range <= 0:
            continue
        clamped = max(min_score, min(float(value), max_score))
        normalized = (clamped - min_score) / score_range * 100.0
        weighted_total += normalized * weight
        total_weight += weight

    if total_weight <= 0:
        return 0
    return round(weighted_total / total_weight)


# ── PostgreSQL helpers ────────────────────────────────────────────────────────


def get_team_and_employees(
    conn, team_name: str, exclude_username: str
) -> tuple[uuid.UUID, list[tuple[uuid.UUID, str]]]:
    """Returns (team_id, [(employee_id, username), ...]) excluding `exclude_username`."""
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM teams WHERE name = %s", (team_name,))
        row = cur.fetchone()
        if not row:
            raise RuntimeError(f"Team '{team_name}' not found — run seed.py first")
        team_id = row[0]

        cur.execute(
            """
            SELECT e.id, u.username
            FROM employees e
            JOIN app_users u ON u.id = e.app_user_id
            JOIN team_memberships tm ON tm.employee_id = e.id
            WHERE tm.team_id = %s
              AND u.username != %s
            ORDER BY u.username
            """,
            (team_id, exclude_username),
        )
        employees = [(r[0], r[1]) for r in cur.fetchall()]

    return team_id, employees


def upsert_employee_result(cur, emp_id: uuid.UUID, form_id: uuid.UUID, score: int):
    cur.execute(
        "SELECT id FROM employee_results WHERE employee_id = %s AND form_id = %s",
        (emp_id, form_id),
    )
    row = cur.fetchone()
    risk_level = classify_risk(score)
    now = datetime.now(timezone.utc)

    if row:
        cur.execute(
            """
            UPDATE employee_results
               SET score = %s, risk_level = %s, calculated_at = %s
             WHERE id = %s
            """,
            (score, risk_level, now, row[0]),
        )
        return

    cur.execute(
        """
        INSERT INTO employee_results (id, employee_id, form_id, score, final_score, risk_level, calculated_at)
        VALUES (%s, %s, %s, %s, NULL, %s, %s)
        """,
        (uuid.uuid4(), emp_id, form_id, score, risk_level, now),
    )


# ── ScyllaDB helpers ──────────────────────────────────────────────────────────


def build_questions(form_data: dict, form_id: uuid.UUID) -> list[dict]:
    """Build question dicts from every section in form_data.json."""
    questions = []
    order = 0
    for section in form_data["sections"]:
        flags = SECTION_FLAGS.get(section["id"], [])
        for q in section["questions"]:
            config: dict[str, str] = {
                "weight": WEIGHT_PER_QUESTION,
                "section": section["id"],
                "sectionLabel": section["label"],
                "inverted": "true" if q.get("inverted") else "false",
            }
            # Likert option labels: option_0 … option_N-1 (answers are 1-indexed)
            for i, label in enumerate(LIKERT_OPTIONS):
                config[f"option_{i}"] = label
            # Clinical flags for auto-score pre-fill
            for flag in flags:
                config[flag] = "true"
            questions.append(
                {
                    "id": uuid.uuid4(),
                    "form_id": form_id,
                    "text": q["text"],
                    "type": "LIKERT",
                    "required": True,
                    "config": config,
                    "display_order": order,
                }
            )
            order += 1
    return questions


def find_form_by_title(session, title: str):
    """Return the form_id UUID if a form with this title exists, else None."""
    rows = list(
        session.execute("SELECT form_id FROM forms WHERE title = %s", (title,))
    )
    return rows[0].form_id if rows else None


def delete_form(session, pg_conn, form_id: uuid.UUID, title: str):
    """Remove a form and all its data from ScyllaDB and PostgreSQL."""
    print(f"  Deleting form '{title}' (id: {form_id}) …")

    # ScyllaDB: form rows — fetch per-employee keys first (composite partition key)
    response_rows = list(
        session.execute(
            "SELECT form_id, employee_id FROM form_responses WHERE form_id = %s",
            (form_id,),
        )
    )
    for r in response_rows:
        session.execute(
            "DELETE FROM form_responses WHERE form_id = %s AND employee_id = %s",
            (r.form_id, r.employee_id),
        )
    print(f"  [ok]   deleted {len(response_rows)} form_response row(s)")

    session.execute("DELETE FROM forms WHERE form_id = %s", (form_id,))
    print("  [ok]   deleted form from ScyllaDB")

    # PostgreSQL: employee_results
    with pg_conn.cursor() as cur:
        cur.execute(
            "DELETE FROM employee_results WHERE form_id = %s", (form_id,)
        )
    pg_conn.commit()
    print("  [ok]   deleted employee_results from PostgreSQL")


def insert_form(session, form_data: dict, form_id: uuid.UUID, team_id: uuid.UUID) -> list[dict]:
    """Insert the form into ScyllaDB and return the question list."""
    title = form_data["title"]
    questions = build_questions(form_data, form_id)

    # Register the question_structure UDT so the driver can serialise it
    QuestionStructure = namedtuple(
        "QuestionStructure",
        ["id", "form_id", "text", "type", "required", "config", "display_order"],
    )
    session.cluster.register_user_type(
        session.keyspace, "question_structure", QuestionStructure
    )

    udt_questions = [
        QuestionStructure(
            id=q["id"],
            form_id=q["form_id"],
            text=q["text"],
            type=q["type"],
            required=q["required"],
            config=q["config"],
            display_order=q["display_order"],
        )
        for q in questions
    ]

    session.execute(
        """
        INSERT INTO forms (form_id, title, description, status, created_by, team_ids, questions)
        VALUES (%s, %s, %s, %s, %s, %s, %s)
        """,
        (
            form_id,
            title,
            FORM_DESCRIPTION,
            "ACTIVE",
            COUNSELOR_USERNAME,
            [team_id],
            udt_questions,
        ),
    )
    print(f"  [ok]   form '{title}' created (id: {form_id})")
    return questions


def insert_responses(
    session,
    pg_conn,
    form_id: uuid.UUID,
    employees: list[tuple[uuid.UUID, str]],
    questions: list[dict],
):
    """Insert form_responses in ScyllaDB and employee_results in PostgreSQL."""
    now = datetime.now(timezone.utc)

    with pg_conn.cursor() as cur:
        for emp_id, username in employees:
            # Answers: random value [1, N] per question (LIKERT is 1-indexed)
            answers = {
                q["id"]: random.randint(1, len(LIKERT_OPTIONS)) for q in questions
            }
            helper_score = calculate_helper_score(answers, questions)
            risk_level = classify_risk(helper_score)

            # ScyllaDB form_response
            existing = list(
                session.execute(
                    "SELECT form_id FROM form_responses WHERE form_id = %s AND employee_id = %s",
                    (form_id, emp_id),
                )
            )
            if existing:
                print(f"  [skip] response for '{username}' already exists")
            else:
                session.execute(
                    """
                    INSERT INTO form_responses (
                        form_id, employee_id, submission_id,
                        answers, text_answers,
                        submitted_at, response_status, closed_at
                    ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                    """,
                    (
                        form_id,
                        emp_id,
                        uuid.uuid4(),
                        answers,
                        {},
                        now,
                        "RESPONDED",
                        None,
                    ),
                )
                print(
                    f"  [ok]   {username} responded"
                    f" (score: {helper_score}, risk: {risk_level})"
                )

            # PostgreSQL employee_result
            upsert_employee_result(cur, emp_id, form_id, helper_score)

    pg_conn.commit()


# ── Main ──────────────────────────────────────────────────────────────────────


def main(db_url: str, scylla_host: str, scylla_port: int, scylla_keyspace: str,
         data_file: Path, reset: bool):
    from cassandra.cluster import Cluster

    form_data = load_form_data(data_file)
    title = form_data["title"]
    print(f"\n=== Seeding form '{title}' ===\n")

    # ── PostgreSQL ────────────────────────────────────────────────────────────
    pg_conn = psycopg2.connect(db_url)
    pg_conn.autocommit = False
    psycopg2.extras.register_uuid()

    # ── ScyllaDB ──────────────────────────────────────────────────────────────
    cluster = Cluster([scylla_host], port=scylla_port)
    session = cluster.connect(scylla_keyspace)

    try:
        # 1. Resolve team + employee IDs from PostgreSQL
        print("-- Resolving team and employees --")
        team_id, employees = get_team_and_employees(
            pg_conn, TEAM_NAME, EXCLUDE_EMPLOYEE_USERNAME
        )
        print(f"  team '{TEAM_NAME}' → {team_id}")
        print(f"  {len(employees)} respondent(s) ('{EXCLUDE_EMPLOYEE_USERNAME}' excluded)")

        # 2. Reset if requested
        if reset:
            print("\n-- Resetting existing form data --")
            existing_id = find_form_by_title(session, title)
            if existing_id:
                delete_form(session, pg_conn, existing_id, title)
            else:
                print("  [skip] form not found — nothing to delete")

        # 3. Insert form
        print("\n-- Creating form --")
        existing_id = find_form_by_title(session, title)
        if existing_id:
            print(f"  [skip] form '{title}' already exists (id: {existing_id})")
            print("         Run with --reset to recreate it.")
            return

        form_id = uuid.uuid4()
        questions = insert_form(session, form_data, form_id, team_id)
        print(f"  {len(questions)} question(s) embedded")

        # 4. Insert responses
        print("\n-- Creating responses --")
        insert_responses(session, pg_conn, form_id, employees, questions)

        print("\n=== Form seed complete ===")

    except Exception:
        pg_conn.rollback()
        raise
    finally:
        pg_conn.close()
        cluster.shutdown()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Seed PsyTrack form into ScyllaDB + PostgreSQL")
    parser.add_argument(
        "--db-url",
        default=DEFAULT_DB_URL,
        help="PostgreSQL connection URL (default: %(default)s)",
    )
    parser.add_argument(
        "--scylla-host",
        default=DEFAULT_SCYLLA_HOST,
        help="ScyllaDB host (default: %(default)s)",
    )
    parser.add_argument(
        "--scylla-port",
        type=int,
        default=DEFAULT_SCYLLA_PORT,
        help="ScyllaDB CQL port (default: %(default)s)",
    )
    parser.add_argument(
        "--scylla-keyspace",
        default=DEFAULT_SCYLLA_KEYSPACE,
        help="ScyllaDB keyspace (default: %(default)s)",
    )
    parser.add_argument(
        "--data",
        default=str(DEFAULT_DATA_FILE),
        help="Path to form_data.json (default: %(default)s)",
    )
    parser.add_argument(
        "--reset",
        action="store_true",
        help="Delete existing form (and its responses) before inserting",
    )
    args = parser.parse_args()

    try:
        main(
            args.db_url,
            args.scylla_host,
            args.scylla_port,
            args.scylla_keyspace,
            Path(args.data),
            args.reset,
        )
    except FileNotFoundError as e:
        print(f"\n[ERROR] Data file not found: {e}", file=sys.stderr)
        sys.exit(1)
    except RuntimeError as e:
        print(f"\n[ERROR] {e}", file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"\n[ERROR] {e}", file=sys.stderr)
        sys.exit(1)
