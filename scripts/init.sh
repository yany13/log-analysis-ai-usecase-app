#!/bin/bash
# ByteBite Initialization Script - Runs all setup tasks
# This script is executed by the adhoc container after services are up

set -e

echo "=========================================="
echo "🚀 ByteBite Initialization Starting"
echo "=========================================="

# Wait for OpenLDAP to be ready
echo "⏳ Waiting for OpenLDAP to be healthy..."
./scripts/wait-for-ldap.sh

# Initialize LDAP
echo "🔐 Initializing LDAP..."
./scripts/init-ldap.sh

# Wait for PostgreSQL
echo "⏳ Waiting for PostgreSQL..."
./scripts/wait-for-postgres.sh

# Initialize Database
echo "📊 Initializing Database..."
./scripts/init-db.sh

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
