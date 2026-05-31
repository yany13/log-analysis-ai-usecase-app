#!/bin/bash
# Initialize PostgreSQL database if needed

DB_HOST=${DB_HOST:-postgres}
DB_PORT=${DB_PORT:-5432}
DB_USER=${DB_USER:-bytebite}
DB_PASSWORD=${DB_PASSWORD:-bytebite_secure_pwd_123}
DB_NAME=${DB_NAME:-bytebite_db}

export PGPASSWORD="$DB_PASSWORD"

echo "Checking if database schema exists..."

# Check if tables already exist
TABLES=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public';" 2>/dev/null | tail -1 | xargs)

if [ "$TABLES" -gt 0 ]; then
  echo "✅ Database schema already initialized ($TABLES tables found)"
  return 0
fi

echo "📊 Database schema not found, initializing..."

# Run init.sql if it exists
if [ -f "./docker/postgres/init.sql" ]; then
  echo "Running init.sql..."
  psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "./docker/postgres/init.sql" > /dev/null 2>&1
  echo "✅ Database initialized successfully"
else
  echo "⚠️  init.sql not found, skipping database initialization"
fi

unset PGPASSWORD
