#!/usr/bin/env python3
"""
Deletes every form created by counselor_carol OR assigned to Equipe Alpha,
together with all associated data:
  - ScyllaDB : forms + form_responses (+ therapist_evaluations if present)
  - PostgreSQL: employee_results

Requirements:
    pip install psycopg2-binary cassandra-driver

Usage:
    python scripts/clear_alpha_forms.py

    python scripts/clear_alpha_forms.py \\
        --db-url postgresql://unformulieren:unformulieren@localhost:5432/unformulieren \\
        --scylla-host localhost --scylla-port 9042 --scylla-keyspace psytrack
"""

import argparse
import sys

import psycopg2
import psycopg2.extras

DEFAULT_DB_URL = "postgresql://unformulieren:unformulieren@localhost:5432/unformulieren"
DEFAULT_SCYLLA_HOST = "localhost"
DEFAULT_SCYLLA_PORT = 9042
DEFAULT_SCYLLA_KEYSPACE = "psytrack"

COUNSELOR_USERNAME = "counselor_carol"
TEAM_NAME = "Equipe Alpha"


# ── PostgreSQL ────────────────────────────────────────────────────────────────


def get_alpha_team_id(conn):
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM teams WHERE name = %s", (TEAM_NAME,))
        row = cur.fetchone()
    if not row:
        raise RuntimeError(f"Team '{TEAM_NAME}' not found — run seed.py first")
    return row[0]


def delete_pg_employee_results(conn, form_ids: list):
    if not form_ids:
        return
    with conn.cursor() as cur:
        cur.execute(
            "DELETE FROM employee_results WHERE form_id = ANY(%s)",
            (form_ids,),
        )
        print(f"  [pg]   deleted {cur.rowcount} employee_result row(s)")
    conn.commit()


# ── ScyllaDB ──────────────────────────────────────────────────────────────────


def find_matching_forms(session, team_id) -> list:
    """
    Return all form rows whose created_by = COUNSELOR_USERNAME
    or whose team_ids list contains team_id.
    Full scan is fine for seed data volumes.
    """
    rows = session.execute("SELECT form_id, title, created_by, team_ids FROM forms")
    matched = []
    for row in rows:
        by_counselor = row.created_by == COUNSELOR_USERNAME
        in_team = row.team_ids and team_id in row.team_ids
        if by_counselor or in_team:
            matched.append(row)
    return matched


def delete_scylla_form(session, form_id, title: str):
    """Delete form_responses, therapist_evaluations, and the form row."""
    # form_responses — composite partition key (form_id, employee_id)
    resp_rows = list(
        session.execute(
            "SELECT form_id, employee_id FROM form_responses WHERE form_id = %s",
            (form_id,),
        )
    )
    for r in resp_rows:
        session.execute(
            "DELETE FROM form_responses WHERE form_id = %s AND employee_id = %s",
            (r.form_id, r.employee_id),
        )

    # therapist_evaluations — same composite key
    eval_rows = list(
        session.execute(
            "SELECT form_id, employee_id FROM therapist_evaluations WHERE form_id = %s",
            (form_id,),
        )
    )
    for r in eval_rows:
        session.execute(
            "DELETE FROM therapist_evaluations WHERE form_id = %s AND employee_id = %s",
            (r.form_id, r.employee_id),
        )

    # form itself
    session.execute("DELETE FROM forms WHERE form_id = %s", (form_id,))

    parts = [f"{len(resp_rows)} response(s)"]
    if eval_rows:
        parts.append(f"{len(eval_rows)} evaluation(s)")
    print(f"  [ok]   '{title}' — deleted {', '.join(parts)}")


# ── Main ──────────────────────────────────────────────────────────────────────


def main(db_url: str, scylla_host: str, scylla_port: int, scylla_keyspace: str):
    from cassandra.cluster import Cluster

    print(f"\n=== Clearing forms for '{TEAM_NAME}' / '{COUNSELOR_USERNAME}' ===\n")

    pg_conn = psycopg2.connect(db_url)
    pg_conn.autocommit = False
    psycopg2.extras.register_uuid()

    cluster = Cluster([scylla_host], port=scylla_port)
    session = cluster.connect(scylla_keyspace)

    try:
        print("-- Resolving Equipe Alpha --")
        team_id = get_alpha_team_id(pg_conn)
        print(f"  team_id: {team_id}")

        print("\n-- Scanning ScyllaDB forms --")
        matched = find_matching_forms(session, team_id)
        if not matched:
            print("  No matching forms found — nothing to delete.")
            return

        print(f"  {len(matched)} form(s) to delete:")
        for row in matched:
            print(f"    • '{row.title}' ({row.form_id})")

        print("\n-- Deleting forms and responses --")
        form_ids = []
        for row in matched:
            delete_scylla_form(session, row.form_id, row.title)
            form_ids.append(row.form_id)

        print("\n-- Deleting employee_results from PostgreSQL --")
        delete_pg_employee_results(pg_conn, form_ids)

        print("\n=== Done — all matching forms cleared ===")

    except Exception:
        pg_conn.rollback()
        raise
    finally:
        pg_conn.close()
        cluster.shutdown()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Delete all forms for Equipe Alpha / counselor_carol"
    )
    parser.add_argument("--db-url", default=DEFAULT_DB_URL)
    parser.add_argument("--scylla-host", default=DEFAULT_SCYLLA_HOST)
    parser.add_argument("--scylla-port", type=int, default=DEFAULT_SCYLLA_PORT)
    parser.add_argument("--scylla-keyspace", default=DEFAULT_SCYLLA_KEYSPACE)
    args = parser.parse_args()

    try:
        main(args.db_url, args.scylla_host, args.scylla_port, args.scylla_keyspace)
    except RuntimeError as e:
        print(f"\n[ERROR] {e}", file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"\n[ERROR] {e}", file=sys.stderr)
        sys.exit(1)
