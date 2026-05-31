#!/bin/bash
# ByteBite startup script - starts Docker and initializes LDAP

set -e

echo "🚀 Starting ByteBite services..."
docker-compose up -d

echo "⏳ Waiting for services to be healthy..."
sleep 10

echo "🔐 Initializing LDAP users..."
bash scripts/init-ldap.sh

echo ""
echo "✅ ByteBite is ready!"
echo "🌐 Access the app: http://localhost:8080/bytebite"
echo "📝 Login with: admin / admin"
echo ""
echo "To stop services: docker-compose down"
