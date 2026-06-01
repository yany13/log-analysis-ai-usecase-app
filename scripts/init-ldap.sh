#!/bin/bash
# Initialize LDAP from bootstrap.ldif file

set -e

LDAP_HOST=${LDAP_HOST:-"openldap"}
LDAP_PORT=${LDAP_PORT:-"389"}
LDAP_ADMIN_DN=${LDAP_ADMIN_DN:-"cn=admin,dc=bytebite,dc=local"}
LDAP_ADMIN_PASSWORD=${LDAP_ADMIN_PASSWORD:-"admin_secure_pwd_123"}
LDAP_BASE_DN=${LDAP_BASE_DN:-"dc=bytebite,dc=local"}

echo "🔐 Initializing LDAP from bootstrap file..."
echo "   Host: $LDAP_HOST:$LDAP_PORT"
echo "   Base DN: $LDAP_BASE_DN"
echo ""

# Debug: Check if LDIF file exists
if [ ! -f /ldif/bootstrap.ldif ]; then
  echo "❌ ERROR: bootstrap.ldif not found at /ldif/bootstrap.ldif"
  exit 1
fi
echo "✅ bootstrap.ldif found"
echo ""

# Build LDAP URI for Alpine compatibility (-h and -p flags don't work, use -H with URI instead)
LDAP_URI="ldap://$LDAP_HOST:$LDAP_PORT"

# Function to check if an entry exists in LDAP
entry_exists() {
  local dn="$1"
  ldapsearch -x -H "$LDAP_URI" \
    -D "$LDAP_ADMIN_DN" -w "$LDAP_ADMIN_PASSWORD" \
    -b "$dn" -s base "(objectClass=*)" >/dev/null 2>&1
}

# Use ldapadd with file input (not stdin piping which doesn't work reliably in containers)
# Continue on errors to allow idempotent runs
echo "Running ldapadd command..."
echo "   LDAP URI: $LDAP_URI"
OUTPUT=$(ldapadd -x -H "$LDAP_URI" \
  -D "$LDAP_ADMIN_DN" -w "$LDAP_ADMIN_PASSWORD" -c -f /ldif/bootstrap.ldif 2>&1 || true)
echo "ldapadd output:"
echo "$OUTPUT"
echo ""

# Count successful additions
ADDED=$(echo "$OUTPUT" | grep -c "adding new entry" || true)
ERRORS=$(echo "$OUTPUT" | grep -c "already exists" || true)

echo "📊 LDAP Import Results:"
echo "   ✅ New entries added: $ADDED"
echo "   ⚠️  Already existing: $ERRORS"

# Verify users were created
echo ""
echo "🔍 Verifying LDAP entries..."

USER_ENTRIES=(
  "uid=admin,ou=people,dc=bytebite,dc=local"
  "uid=manager,ou=people,dc=bytebite,dc=local"
  "uid=staff1,ou=people,dc=bytebite,dc=local"
  "uid=staff2,ou=people,dc=bytebite,dc=local"
  "uid=kds_operator,ou=people,dc=bytebite,dc=local"
  "uid=customer1,ou=people,dc=bytebite,dc=local"
)

VERIFIED=0
for user_dn in "${USER_ENTRIES[@]}"; do
  if entry_exists "$user_dn"; then
    USER_ID=$(echo "$user_dn" | sed 's/uid=\([^,]*\).*/\1/')
    echo "   ✅ $USER_ID found in LDAP"
    ((VERIFIED++))
  else
    USER_ID=$(echo "$user_dn" | sed 's/uid=\([^,]*\).*/\1/')
    echo "   ❌ $USER_ID NOT found in LDAP"
  fi
done

echo ""
if [ "$VERIFIED" -eq 6 ]; then
  echo "✅ LDAP initialization complete! All users verified."
else
  echo "⚠️  LDAP initialization partial. $VERIFIED/6 users found."
  echo ""
  echo "📝 Available users (all with password 'admin'):"
  echo "   - admin (Administrator)"
  echo "   - manager (Operations Manager)"
  echo "   - staff1 (Staff Member)"
  echo "   - staff2 (Staff Member)"
  echo "   - kds_operator (Kitchen Display System)"
  echo "   - customer1 (Customer)"
fi
echo ""
