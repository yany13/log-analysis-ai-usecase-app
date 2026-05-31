#!/bin/bash
# Wait for OpenLDAP to be ready

LDAP_HOST=${LDAP_HOST:-openldap}
LDAP_PORT=${LDAP_PORT:-389}
MAX_ATTEMPTS=30
ATTEMPT=0

echo "Checking LDAP at $LDAP_HOST:$LDAP_PORT..."

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
  # Simple port check using nc (netcat) or timeout
  if timeout 2 bash -c ">/dev/tcp/$LDAP_HOST/$LDAP_PORT" 2>/dev/null; then
    echo "✅ LDAP port is open and responding!"
    sleep 2  # Extra wait for LDAP to fully initialize
    exit 0
  fi

  ATTEMPT=$((ATTEMPT + 1))
  echo "⏳ Waiting for LDAP... (attempt $ATTEMPT/$MAX_ATTEMPTS)"
  sleep 2
done

echo "❌ LDAP failed to become ready after $MAX_ATTEMPTS attempts"
exit 1
