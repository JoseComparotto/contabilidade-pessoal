#!/bin/sh
set -e

# Environment variables expected (with defaults for dev)
APP_DB_USER=${APP_DB_USER:-app}
APP_DB_PASSWORD=${APP_DB_PASSWORD:-app}
POSTGRES_DB=${POSTGRES_DB:-postgres}

# Escape single quotes for SQL literal
APP_DB_PASSWORD_ESC=`printf %s "$APP_DB_PASSWORD" | sed "s/'/''/g"`

# Create role if it does not exist
if ! psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -tAc "SELECT 1 FROM pg_roles WHERE rolname = '$APP_DB_USER'" | grep -q 1; then
   psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -c "CREATE ROLE \"$APP_DB_USER\" LOGIN PASSWORD '$APP_DB_PASSWORD_ESC'"
fi

# Grant connect on current database
psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -c "GRANT CONNECT ON DATABASE \"$POSTGRES_DB\" TO \"$APP_DB_USER\""

# Grant usage on schema public
psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -c "GRANT USAGE ON SCHEMA public TO \"$APP_DB_USER\""
