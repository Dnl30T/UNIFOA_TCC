#!/usr/bin/env python3
"""
Toggles the `fully_anonymized` flag for every employee in Equipe Alpha.

Current state → new state:
  FALSE → TRUE   (anonymize)
  TRUE  → FALSE  (de-anonymize)

The toggle is applied to all employees whose account has role EMPLOYEE and
who are members of Equipe Alpha.

Usage:
    python scripts/toggle_alpha_anonymized.py

    # Force a specific state instead of toggling
    python scripts/toggle_alpha_anonymized.py --set true
    python scripts/toggle_alpha_anonymized.py --set false

    # Custom DB URL
    python scripts/toggle_alpha_anonymized.py \\
        --db-url postgresql://unformulieren:unformulieren@localhost:5432/unformulieren
"""

import argparse
import sys

import psycopg2
import psycopg2.extras

DEFAULT_DB_URL = "postgresql://unformulieren:unformulieren@localhost:5432/unformulieren"
TEAM_NAME = "Equipe Alpha"


def main(db_url: str, force: bool | None):
    conn = psycopg2.connect(db_url)
    conn.autocommit = False
    psycopg2.extras.register_uuid()

    try:
        with conn.cursor() as cur:
            # Resolve team
            cur.execute("SELECT id FROM teams WHERE name = %s", (TEAM_NAME,))
            row = cur.fetchone()
            if not row:
                raise RuntimeError(f"Team '{TEAM_NAME}' not found — run seed.py first")
            team_id = row[0]

            # Fetch all employees in the team with their current flag
            cur.execute(
                """
                SELECT u.id, u.username, u.fully_anonymized
                FROM app_users u
                JOIN employees e ON e.app_user_id = u.id
                JOIN team_memberships tm ON tm.employee_id = e.id
                WHERE tm.team_id = %s
                  AND u.role = 'EMPLOYEE'
                ORDER BY u.username
                """,
                (team_id,),
            )
            members = cur.fetchall()

            if not members:
                print("No employees found in Equipe Alpha.")
                return

            print(f"\n=== Toggling fully_anonymized for {len(members)} employee(s) in '{TEAM_NAME}' ===\n")

            for user_id, username, current in members:
                if force is None:
                    new_value = not current
                else:
                    new_value = force

                cur.execute(
                    "UPDATE app_users SET fully_anonymized = %s, updated_at = NOW() WHERE id = %s",
                    (new_value, user_id),
                )
                arrow = f"{current} → {new_value}"
                print(f"  {'[ok]  ' if current != new_value else '[skip]'} {username:<20} {arrow}")

        conn.commit()
        print("\n=== Done ===")

    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description=f"Toggle fully_anonymized for all employees in '{TEAM_NAME}'"
    )
    parser.add_argument(
        "--db-url",
        default=DEFAULT_DB_URL,
        help="PostgreSQL connection URL (default: %(default)s)",
    )
    parser.add_argument(
        "--set",
        dest="force",
        choices=["true", "false"],
        default=None,
        help="Force a specific value instead of toggling",
    )
    args = parser.parse_args()

    force_value = None
    if args.force == "true":
        force_value = True
    elif args.force == "false":
        force_value = False

    try:
        main(args.db_url, force_value)
    except RuntimeError as e:
        print(f"\n[ERROR] {e}", file=sys.stderr)
        sys.exit(1)
    except psycopg2.OperationalError as e:
        print(f"\n[ERROR] Could not connect to database: {e}", file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"\n[ERROR] {e}", file=sys.stderr)
        sys.exit(1)
