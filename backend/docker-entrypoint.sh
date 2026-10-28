#!/bin/sh
set -eu

# Render Postgres exposes credentials inside a PostgreSQL URI. The JDBC driver
# expects credentials in DB_USERNAME/DB_PASSWORD, so remove URI user-info and
# keep only the host, port, database, and optional query string in DB_URL.
if [ -n "${DATABASE_URL:-}" ]; then
    case "$DATABASE_URL" in
        jdbc:postgresql://*) postgres_url="${DATABASE_URL#jdbc:}" ;;
        postgresql://*) postgres_url="$DATABASE_URL" ;;
        postgres://*) postgres_url="postgresql://${DATABASE_URL#postgres://}" ;;
        *) echo "DATABASE_URL must use postgres:// or postgresql://." >&2; exit 1 ;;
    esac
    authority_and_path="${postgres_url#postgresql://}"
    authority="${authority_and_path%%/*}"
    path="${authority_and_path#"$authority"}"
    host_port="${authority##*@}"
    export DB_URL="jdbc:postgresql://${host_port}${path}"
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
