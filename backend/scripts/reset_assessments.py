#!/usr/bin/env python3
"""
Full reset: wipe all data and restore only seed_data.json.

After this script runs, both datastores contain exactly what seed_data.json
defines (managers, counselors, teams, employees) and nothing else.

What gets deleted:
- PostgreSQL: all application tables in FK-safe order (results, snapshots,
  forms, responses, memberships, employees, teams, users).
- Scylla/Cassandra: forms, form_responses, therapist_evaluations.

After the wipe, seed.py is called automatically to re-populate users and teams.

Usage:
  python scripts/reset_assessments.py

  python scripts/reset_assessments.py \
    --db-url postgresql://unformulieren:unformulieren@localhost:5432/unformulieren \
    --scylla-host localhost --scylla-port 9042 --scylla-keyspace psytrack

  # Reset only one datastore (--skip-postgres also skips the re-seed step)
  python scripts/reset_assessments.py --skip-scylla
  python scripts/reset_assessments.py --skip-postgres
"""

import argparse
import subprocess
import sys
from pathlib import Path

import psycopg2

DEFAULT_DB_URL = "postgresql://unformulieren:unformulieren@localhost:5432/unformulieren"

# FK-safe deletion order: every child table comes before its parent.
# flyway_schema_history is intentionally excluded (system table).
POSTGRES_DELETE_ORDER = [
    "team_result_risk_level_distribution",  # → team_results
    "team_results",
    "dashboard_snapshots",
    "employee_results",
    "form_responses",
    "question_configs",                     # → questions (CASCADE, but explicit)
    "questions",
    "forms",
    "team_memberships",                     # → teams (CASCADE, but explicit)
    "employees",                            # → app_users (CASCADE, but explicit)
    "teams",                                # → app_users (no cascade on manager_id)
    "app_users",
]

SCYLLA_TRUNCATE_TABLES = [
    "therapist_evaluations",
    "form_responses",
    "forms",
]


def reset_postgres(db_url: str) -> None:
    print("-- Wiping PostgreSQL --")
    conn = psycopg2.connect(db_url)
    conn.autocommit = False
    try:
        with conn.cursor() as cur:
            for table in POSTGRES_DELETE_ORDER:
                cur.execute(f"DELETE FROM {table}")
                print(f"  [ok] deleted {table}")
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def reset_scylla(host: str, port: int, keyspace: str, username: str | None, password: str | None) -> None:
    print("-- Wiping ScyllaDB --")
    try:
        from cassandra.cluster import Cluster
        from cassandra.auth import PlainTextAuthProvider
    except ImportError as exc:
        raise RuntimeError(
            "cassandra-driver is required for Scylla reset. Install with: pip install cassandra-driver"
        ) from exc

    auth = PlainTextAuthProvider(username=username, password=password) if username and password else None
    cluster = Cluster([host], port=port, auth_provider=auth)
    session = cluster.connect()
    try:
        session.set_keyspace(keyspace)
        for table in SCYLLA_TRUNCATE_TABLES:
            session.execute(f"TRUNCATE {table}")
            print(f"  [ok] truncated {keyspace}.{table}")
    finally:
        session.shutdown()
        cluster.shutdown()


def run_seed(db_url: str) -> None:
    print("-- Re-seeding from seed_data.json --")
    seed_script = Path(__file__).parent / "seed.py"
    subprocess.run(
        [sys.executable, str(seed_script), "--db-url", db_url],
        check=True,
    )


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Full reset: wipe all data and restore only seed_data.json"
    )
    parser.add_argument("--db-url", default=DEFAULT_DB_URL, help="PostgreSQL URL")
    parser.add_argument("--skip-postgres", action="store_true", help="Skip PostgreSQL wipe and re-seed")
    parser.add_argument("--skip-scylla", action="store_true", help="Skip Scylla/Cassandra truncate")
    parser.add_argument("--scylla-host", default="localhost", help="Scylla host")
    parser.add_argument("--scylla-port", default=9042, type=int, help="Scylla port")
    parser.add_argument("--scylla-keyspace", default="psytrack", help="Scylla keyspace")
    parser.add_argument("--scylla-username", default=None, help="Scylla username")
    parser.add_argument("--scylla-password", default=None, help="Scylla password")
    args = parser.parse_args()

    if args.skip_postgres and args.skip_scylla:
        print("Nothing to do: both datastore resets were skipped.")
        return

    try:
        if not args.skip_postgres:
            reset_postgres(args.db_url)
            run_seed(args.db_url)
        if not args.skip_scylla:
            reset_scylla(
                args.scylla_host,
                args.scylla_port,
                args.scylla_keyspace,
                args.scylla_username,
                args.scylla_password,
            )
        print("\nReset complete: only seed_data.json data remains.")
    except psycopg2.OperationalError as exc:
        print(f"\n[ERROR] PostgreSQL connection failed: {exc}", file=sys.stderr)
        sys.exit(1)
    except Exception as exc:
        print(f"\n[ERROR] {exc}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
