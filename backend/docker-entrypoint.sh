#!/bin/sh
set -eu

# Render Postgres exposes a PostgreSQL URL; Spring's JDBC driver requires the
# jdbc:postgresql:// scheme. Normalize both common PostgreSQL URL schemes.
if [ -n "${DATABASE_URL:-}" ]; then
    case "$DATABASE_URL" in
        jdbc:postgresql://*) export DB_URL="$DATABASE_URL" ;;
        postgresql://*) export DB_URL="jdbc:$DATABASE_URL" ;;
        postgres://*) export DB_URL="jdbc:postgresql://${DATABASE_URL#postgres://}" ;;
        *) echo "DATABASE_URL must use postgres:// or postgresql://." >&2; exit 1 ;;
    esac
fi

if [ -z "${DB_URL:-}" ]; then
    echo "Database URL is missing; set DATABASE_URL or DB_URL." >&2
    exit 1
fi

case "$DB_URL" in
    jdbc:postgresql://*) echo "PostgreSQL JDBC URL configured." ;;
    *) echo "DB_URL must use the jdbc:postgresql:// scheme." >&2; exit 1 ;;
esac

exec java -jar /app/app.jar
