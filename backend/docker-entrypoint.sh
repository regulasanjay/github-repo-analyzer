#!/bin/sh
set -eu

# Render Postgres exposes a standard PostgreSQL URL; Spring's JDBC driver needs
# the same URL prefixed with jdbc:.
if [ -n "${DATABASE_URL:-}" ] && [ -z "${DB_URL:-}" ]; then
    case "$DATABASE_URL" in
        jdbc:*) export DB_URL="$DATABASE_URL" ;;
        postgres://*|postgresql://*) export DB_URL="jdbc:$DATABASE_URL" ;;
    esac
fi

exec java -jar /app/app.jar
