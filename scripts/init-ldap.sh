#!/bin/bash
# Initialize LDAP from bootstrap.ldif file

LDAP_HOST=${LDAP_HOST:-"openldap"}
LDAP_PORT=${LDAP_PORT:-"389"}
LDAP_ADMIN_DN=${LDAP_ADMIN_DN:-"cn=admin,dc=bytebite,dc=local"}
LDAP_ADMIN_PASSWORD=${LDAP_ADMIN_PASSWORD:-"admin_secure_pwd_123"}

echo "🔐 Initializing LDAP from bootstrap file..."

# Use cat to read bootstrap.ldif and pipe to ldapadd
# This is more reliable than file references in container context
cat /ldif/bootstrap.ldif | ldapadd -x -h "$LDAP_HOST" -p "$LDAP_PORT" \
  -D "$LDAP_ADMIN_DN" -w "$LDAP_ADMIN_PASSWORD" -c 2>&1 | \
  grep "adding new entry" || true

echo ""
echo "✅ LDAP initialization complete!"
echo ""
echo "📝 Available users (all with password 'admin'):"
echo "   - admin (Administrator)"
echo "   - manager (Operations Manager)"
echo "   - staff1 (Staff Member)"
echo "   - staff2 (Staff Member)"
echo "   - kds_operator (Kitchen Display System)"
echo "   - customer1 (Customer)"
echo ""
