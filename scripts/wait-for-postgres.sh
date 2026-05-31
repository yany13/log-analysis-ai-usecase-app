#!/bin/bash
# Wait for PostgreSQL to be ready

DB_HOST=${DB_HOST:-postgres}
DB_PORT=${DB_PORT:-5432}
DB_USER=${DB_USER:-bytebite}
DB_PASSWORD=${DB_PASSWORD:-bytebite_secure_pwd_123}
DB_NAME=${DB_NAME:-bytebite_db}
MAX_ATTEMPTS=30
ATTEMPT=0

echo "Checking PostgreSQL at $DB_HOST:$DB_PORT..."

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
  if pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" > /dev/null 2>&1; then
    echo "✅ PostgreSQL is ready!"
    exit 0
  fi

  ATTEMPT=$((ATTEMPT + 1))
  echo "⏳ Waiting for PostgreSQL... (attempt $ATTEMPT/$MAX_ATTEMPTS)"
  sleep 2
done

echo "❌ PostgreSQL failed to become ready after $MAX_ATTEMPTS attempts"
exit 1
