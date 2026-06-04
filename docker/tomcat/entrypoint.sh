#!/bin/bash
# ByteBite Tomcat Entrypoint - Handles initialization and startup
# This script runs LDAP/Database initialization before starting Tomcat

set -e

echo "=========================================="
echo "🚀 ByteBite Startup - Initialization Phase"
echo "=========================================="

# Source environment variables
export LDAP_HOST=${LDAP_HOST:-openldap}
export LDAP_PORT=${LDAP_PORT:-389}
export LDAP_ADMIN_DN=${LDAP_ADMIN_DN:-"cn=admin,dc=bytebite,dc=local"}
export LDAP_ADMIN_PASSWORD=${LDAP_ADMIN_PASSWORD:-admin_secure_pwd_123}
export LDAP_BASE_DN=${LDAP_BASE_DN:-"dc=bytebite,dc=local"}
export DB_HOST=${DB_HOST:-postgres}
export DB_PORT=${DB_PORT:-5432}
export DB_NAME=${DB_NAME:-bytebite_db}
export DB_USER=${DB_USER:-bytebite}
export DB_PASSWORD=${DB_PASSWORD:-bytebite_secure_pwd_123}

# Wait for OpenLDAP to be ready
echo "⏳ Waiting for OpenLDAP to be healthy..."
MAX_ATTEMPTS=30
ATTEMPT=0
while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
  if timeout 2 bash -c ">/dev/tcp/$LDAP_HOST/$LDAP_PORT" 2>/dev/null; then
    echo "✅ LDAP port is open and responding!"
    sleep 2
    break
  fi
  ATTEMPT=$((ATTEMPT + 1))
  echo "⏳ Waiting for LDAP... (attempt $ATTEMPT/$MAX_ATTEMPTS)"
  sleep 2
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
  echo "❌ LDAP failed to become ready"
  exit 1
fi

# Initialize LDAP from bootstrap.ldif
echo "🔐 Initializing LDAP..."
LDAP_URI="ldap://$LDAP_HOST:$LDAP_PORT"

if [ ! -f /ldif/bootstrap.ldif ]; then
  echo "❌ ERROR: bootstrap.ldif not found at /ldif/bootstrap.ldif"
  exit 1
fi

echo "Running ldapadd command..."
OUTPUT=$(ldapadd -x -H "$LDAP_URI" \
  -D "$LDAP_ADMIN_DN" -w "$LDAP_ADMIN_PASSWORD" -c -f /ldif/bootstrap.ldif 2>&1 || true)
echo "$OUTPUT"

ADDED=$(echo "$OUTPUT" | grep -c "adding new entry" || true)
ERRORS=$(echo "$OUTPUT" | grep -c "already exists" || true)
echo "📊 LDAP Import Results: ✅ Added=$ADDED, ⚠️  Existing=$ERRORS"

# Verify LDAP users
echo "🔍 Verifying LDAP entries..."
VERIFIED=0
for user in admin manager staff1 staff2 kds_operator customer1; do
  USER_DN="uid=$user,ou=people,dc=bytebite,dc=local"
  if ldapsearch -x -H "$LDAP_URI" \
    -D "$LDAP_ADMIN_DN" -w "$LDAP_ADMIN_PASSWORD" \
    -b "$USER_DN" -s base "(objectClass=*)" >/dev/null 2>&1; then
    echo "   ✅ $user found in LDAP"
    VERIFIED=$((VERIFIED + 1))
  fi
done

echo "✅ LDAP initialization complete! ($VERIFIED/6 users verified)"

# Wait for PostgreSQL
echo "⏳ Waiting for PostgreSQL..."
export PGPASSWORD="$DB_PASSWORD"
ATTEMPT=0
while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
  if pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" > /dev/null 2>&1; then
    echo "✅ PostgreSQL is ready!"
    break
  fi
  ATTEMPT=$((ATTEMPT + 1))
  echo "⏳ Waiting for PostgreSQL... (attempt $ATTEMPT/$MAX_ATTEMPTS)"
  sleep 2
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
  echo "❌ PostgreSQL failed to become ready"
  exit 1
fi

# Initialize Database if schema doesn't exist
echo "📊 Checking database schema..."
TABLES=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public';" 2>/dev/null | tail -1 | xargs)

if [ "$TABLES" -gt 0 ]; then
  echo "✅ Database schema already initialized ($TABLES tables found)"
else
  echo "📊 Database schema not found, initializing..."
  if [ -f /usr/local/tomcat/docker-init/init.sql ]; then
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f /usr/local/tomcat/docker-init/init.sql > /dev/null 2>&1
    echo "✅ Database initialized successfully"
  else
    echo "⚠️  init.sql not found, skipping database initialization"
  fi
fi

unset PGPASSWORD

echo ""
echo "=========================================="
echo "✅ ByteBite Initialization Complete!"
echo "=========================================="
echo ""
echo "🌐 Application ready at:"
echo "   http://localhost:8080/bytebite"
echo ""
echo "📝 Default credentials:"
echo "   Username: admin"
echo "   Password: admin"
echo ""
echo "🚀 Starting Apache Tomcat..."
echo "=========================================="
echo ""

# Start Tomcat
exec catalina.sh run
