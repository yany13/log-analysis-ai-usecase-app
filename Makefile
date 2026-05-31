.PHONY: start stop restart logs init-ldap help

help:
	@echo "ByteBite Commands:"
	@echo "  make start       - Start all services with LDAP initialization"
	@echo "  make stop        - Stop all services"
	@echo "  make restart     - Restart all services"
	@echo "  make init-ldap   - Initialize LDAP users (if already running)"
	@echo "  make logs        - View Docker logs"

start:
	@echo "🚀 Starting ByteBite services..."
	docker-compose up -d
	@echo "⏳ Waiting for services to be healthy..."
	@sleep 10
	@echo "🔐 Initializing LDAP users..."
	@bash scripts/init-ldap.sh
	@echo ""
	@echo "✅ ByteBite is ready!"
	@echo "🌐 Access: http://localhost:8080/bytebite"
	@echo "📝 Login: admin / admin"

stop:
	docker-compose down

restart: stop start

init-ldap:
	bash scripts/init-ldap.sh

logs:
	docker-compose logs -f
