#!/usr/bin/env python3
"""
Seed script — populates the PostgreSQL database directly with fake users.

Requirements:
    pip install psycopg2-binary bcrypt

Usage:
    # Uses default connection settings (matches docker-compose defaults)
    python scripts/seed.py

    # Custom database URL
    python scripts/seed.py --db-url postgresql://unformulieren:unformulieren@localhost:5432/unformulieren

    # Custom data file
    python scripts/seed.py --data scripts/data/seed_data.json

    # Wipe existing seed data before inserting
    python scripts/seed.py --reset
"""

import argparse
import json
import sys
import uuid
import random
import string
from pathlib import Path

import bcrypt
import psycopg2
import psycopg2.extras

# ── Config ────────────────────────────────────────────────────────────────────

DEFAULT_DATA_FILE = Path(__file__).parent / "data" / "seed_data.json"
DEFAULT_DB_URL = "postgresql://unformulieren:unformulieren@localhost:5432/unformulieren"

# ── Helpers ───────────────────────────────────────────────────────────────────

def load_data(path: Path) -> dict:
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def hash_password(plain: str) -> str:
    # Python bcrypt produces $2b$ hashes; Spring's BCryptPasswordEncoder uses
    # $2a$. Both are the same algorithm — normalise to $2a$ for compatibility.
    raw = bcrypt.hashpw(plain.encode(), bcrypt.gensalt()).decode()
    return raw.replace("$2b$", "$2a$", 1)


def gen_team_code(length: int = 6) -> str:
    return "".join(random.choices(string.ascii_uppercase + string.digits, k=length))


def unique_team_code(cur) -> str:
    """Generate a team code that does not already exist in the database."""
    while True:
        code = gen_team_code()
        cur.execute("SELECT 1 FROM teams WHERE team_code = %s", (code,))
        if cur.fetchone() is None:
            return code


# ── Seed logic ────────────────────────────────────────────────────────────────

def upsert_user(cur, user: dict, role: str) -> uuid.UUID:
    """Insert a user, skip if username already exists. Returns the user id."""
    cur.execute("SELECT id FROM app_users WHERE username = %s", (user["username"],))
    row = cur.fetchone()
    if row:
        print(f"  [skip] user '{user['username']}' already exists")
        return row[0]

    user_id = uuid.uuid4()
    cur.execute(
        """
        INSERT INTO app_users (
            id, username, password_hash, email, role,
            name, phone_number, company, job_title,
            consent_given, consent_date,
            created_at, updated_at, anonymized
        ) VALUES (
            %s, %s, %s, %s, %s,
            %s, %s, %s, %s,
            TRUE, NOW(),
            NOW(), NOW(), FALSE
        )
        """,
        (
            user_id,
            user["username"],
            hash_password(user["password"]),
            user["email"],
            role,
            user.get("name"),
            user.get("phoneNumber"),
            user.get("company"),
            user.get("jobTitle"),
        ),
    )
    print(f"  [ok]   {role.lower()} '{user['username']}' ({user['email']})")
    return user_id


def upsert_team(cur, name: str, manager_id: uuid.UUID, counselor_id: uuid.UUID | None = None) -> tuple[uuid.UUID, str]:
    """Insert a team, skip if name already exists. Returns (team_id, team_code)."""
    cur.execute("SELECT id, team_code FROM teams WHERE name = %s", (name,))
    row = cur.fetchone()
    if row:
        # Update counselor_id if provided and not yet set
        if counselor_id:
            cur.execute(
                "UPDATE teams SET counselor_id = %s, updated_at = NOW() WHERE id = %s AND counselor_id IS NULL",
                (counselor_id, row[0]),
            )
        print(f"  [skip] team '{name}' already exists (code: {row[1]})")
        return row[0], row[1]

    team_id = uuid.uuid4()
    code = unique_team_code(cur)
    cur.execute(
        """
        INSERT INTO teams (id, name, manager_id, counselor_id, team_code, created_at, updated_at)
        VALUES (%s, %s, %s, %s, %s, NOW(), NOW())
        """,
        (team_id, name, manager_id, counselor_id, code),
    )
    print(f"  [ok]   team '{name}' (code: {code})")
    return team_id, code


def upsert_employee(cur, user_id: uuid.UUID, team_id: uuid.UUID):
    """Insert an employee row linked to an app_user and team."""
    cur.execute("SELECT id FROM employees WHERE app_user_id = %s", (user_id,))
    if cur.fetchone():
        return  # already linked

    emp_id = uuid.uuid4()
    cur.execute(
        """
        INSERT INTO employees (id, name, team_id, status, app_user_id)
        VALUES (%s, (SELECT name FROM app_users WHERE id = %s), %s, 'ACTIVE', %s)
        """,
        (emp_id, user_id, team_id, user_id),
    )


def upsert_membership(cur, team_id: uuid.UUID, employee_id: uuid.UUID):
    """Insert a team_membership row, skip if already present."""
    cur.execute(
        "SELECT 1 FROM team_memberships WHERE team_id = %s AND employee_id = %s",
        (team_id, employee_id),
    )
    if cur.fetchone():
        return

    cur.execute(
        "INSERT INTO team_memberships (id, team_id, employee_id, joined_at) VALUES (%s, %s, %s, NOW())",
        (uuid.uuid4(), team_id, employee_id),
    )


def reset_seed_data(cur):
    """Remove all rows inserted by this script (identified by @psytrack.dev emails)."""
    print("  Deleting existing seed data...")
    cur.execute(
        """
        DELETE FROM team_memberships
        WHERE employee_id IN (
            SELECT e.id FROM employees e
            JOIN app_users u ON u.id = e.app_user_id
            WHERE u.email LIKE '%@psytrack.dev'
        )
        """
    )
    cur.execute(
        """
        DELETE FROM employees
        WHERE app_user_id IN (
            SELECT id FROM app_users WHERE email LIKE '%@psytrack.dev'
        )
        """
    )
    cur.execute(
        """
        DELETE FROM teams
        WHERE manager_id IN (
            SELECT id FROM app_users WHERE email LIKE '%@psytrack.dev'
        )
        """
    )
    cur.execute("DELETE FROM app_users WHERE email LIKE '%@psytrack.dev'")
    print("  [ok]   reset complete")


# ── Main ──────────────────────────────────────────────────────────────────────

def main(db_url: str, data_file: Path, reset: bool):
    data = load_data(data_file)
    print(f"\n=== Seeding database directly (data: {data_file}) ===\n")

    conn = psycopg2.connect(db_url)
    conn.autocommit = False
    psycopg2.extras.register_uuid()

    try:
        with conn.cursor() as cur:
            if reset:
                print("-- Resetting seed data --")
                reset_seed_data(cur)
                print()

            # 1. Managers
            print("-- Creating managers --")
            manager_ids: dict[str, uuid.UUID] = {}
            for mgr in data["managers"]:
                user_id = upsert_user(cur, mgr, "MANAGER")
                manager_ids[mgr["email"]] = user_id

            # 2. Counselors
            print("\n-- Creating counselors --")
            counselor_ids: dict[str, uuid.UUID] = {}
            for cns in data["counselors"]:
                user_id = upsert_user(cur, cns, "COUNSELOR")
                counselor_ids[cns["email"]] = user_id

            # 3. Teams + employees
            print("\n-- Creating teams & employees --")
            for team in data["teams"]:
                mgr_id = manager_ids.get(team["managerEmail"])
                if not mgr_id:
                    # Manager may have already existed before this run
                    cur.execute(
                        "SELECT id FROM app_users WHERE email = %s", (team["managerEmail"],)
                    )
                    row = cur.fetchone()
                    if not row:
                        print(f"  [warn] manager {team['managerEmail']} not found — skipping team '{team['name']}'")
                        continue
                    mgr_id = row[0]

                counselor_id = counselor_ids.get(team.get("counselorEmail", ""))
                if not counselor_id and team.get("counselorEmail"):
                    cur.execute(
                        "SELECT id FROM app_users WHERE email = %s", (team["counselorEmail"],)
                    )
                    row = cur.fetchone()
                    counselor_id = row[0] if row else None

                team_id, _ = upsert_team(cur, team["name"], mgr_id, counselor_id)

                for emp_data in team.get("employees", []):
                    emp_user_id = upsert_user(cur, emp_data, "EMPLOYEE")
                    upsert_employee(cur, emp_user_id, team_id)

                    # employee row id
                    cur.execute("SELECT id FROM employees WHERE app_user_id = %s", (emp_user_id,))
                    emp_row = cur.fetchone()
                    if emp_row:
                        upsert_membership(cur, team_id, emp_row[0])

        conn.commit()
        print("\n=== Seed complete ===")

    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Seed PsyTrack database directly")
    parser.add_argument(
        "--db-url",
        default=DEFAULT_DB_URL,
        help="PostgreSQL connection URL (default: %(default)s)",
    )
    parser.add_argument(
        "--data",
        default=str(DEFAULT_DATA_FILE),
        help="Path to seed data JSON file (default: %(default)s)",
    )
    parser.add_argument(
        "--reset",
        action="store_true",
        help="Delete existing @psytrack.dev rows before inserting",
    )
    args = parser.parse_args()

    try:
        main(args.db_url, Path(args.data), args.reset)
    except FileNotFoundError as e:
        print(f"\n[ERROR] Data file not found: {e}", file=sys.stderr)
        sys.exit(1)
    except psycopg2.OperationalError as e:
        print(f"\n[ERROR] Could not connect to database: {e}", file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"\n[ERROR] {e}", file=sys.stderr)
        sys.exit(1)
