# ByteBite Quick Reference

## 🚀 Quick Start (30 seconds)

```bash
# 1. Navigate to project
cd d:\workspace\ai\code\log-analysis-ai-usecase-app

# 2. Start services
docker-compose up -d

# 3. Access application
# Open browser: http://localhost:8080/bytebite/login
# Username: admin
# Password: admin
```

## 📋 Common Commands

### Docker Management
```bash
docker-compose up -d              # Start all services
docker-compose down               # Stop all services
docker-compose logs -f tomcat     # View Tomcat logs
docker-compose ps                 # Check service status
docker-compose restart tomcat     # Restart Tomcat
```

### Database Access
```bash
docker exec -it bytebite-postgres psql -U bytebite -d bytebite_db
SELECT * FROM bytebite.users;
SELECT * FROM bytebite.orders LIMIT 10;
```

### Testing
```bash
mvn test                           # Run unit tests
mvn clean package                  # Build WAR file
curl http://localhost:8080/bytebite/health  # Health check
```

## 🎯 Features

| Feature | URL | Status |
|---------|-----|--------|
| Login | `/login` | ✅ |
| Menu | `/menu` | ✅ |
| Checkout | `/checkout` | ✅ |
| Analytics | `/analytics` | ✅ |
| KDS | `/kds` | ✅ |
| API | `/api/*` | ✅ |
| Health | `/health` | ✅ |

## 🔧 Configuration

### Environment Variables (docker-compose.yml)

```yaml
DB_HOST=postgres
DB_PORT=5432
DB_NAME=bytebite_db
DB_USER=bytebite
DB_PASSWORD=bytebite_secure_pwd_123

LDAP_HOST=openldap
LDAP_PORT=389
LDAP_BASE_DN=dc=bytebite,dc=local
LDAP_BIND_DN=cn=admin,dc=bytebite,dc=local
LDAP_BIND_PASSWORD=admin_secure_pwd_123
```

## 📊 Architecture

```
┌─────────────────────────────────────┐
│         Web Browser                 │
│  (Login, Menu, Checkout, KDS)       │
└────────────┬────────────────────────┘
             │ HTTP/HTTPS
┌────────────▼────────────────────────┐
│      Tomcat 11 + JDK 21             │
│  ┌─────────────────────────────┐   │
│  │ Servlets (6)                │   │
│  │ Filters (4)                 │   │
│  │ Services (3)                │   │
│  └─────────────────────────────┘   │
└────────────┬──────────┬─────────────┘
             │ JDBC     │ LDAP
      ┌──────▼──┐  ┌────▼─────┐
      │ Postgres│  │ OpenLDAP │
      │   15    │  │   1.5    │
      └─────────┘  └──────────┘
```

## ✅ Checklist Before Deploy

- [ ] Docker installed
- [ ] Docker Compose installed
- [ ] Port 8080 available
- [ ] Port 5432 available
- [ ] Port 389 available
- [ ] At least 2GB free disk space
- [ ] At least 2GB free RAM

## 🐛 Troubleshooting

**Services won't start?**
```bash
docker-compose down -v
docker-compose up -d
```

**Database connection error?**
```bash
docker logs bytebite-postgres
docker exec bytebite-postgres pg_isready
```

**LDAP auth failing?**
```bash
docker logs bytebite-openldap
docker exec bytebite-openldap ldapsearch -x -b "dc=bytebite,dc=local"
```

**Tomcat not responding?**
```bash
docker logs bytebite-tomcat
docker restart bytebite-tomcat
```

## 📚 Documentation

- `DEPLOYMENT.md` - Complete deployment guide
- `README.md` - Project overview
- `docker-compose.yml` - Container configuration
- `pom.xml` - Maven dependencies

## 🔗 Links

| Component | URL | User |
|-----------|-----|------|
| Tomcat | http://localhost:8080 | - |
| PostgreSQL | localhost:5432 | bytebite |
| OpenLDAP | localhost:389 | cn=admin,dc=bytebite,dc=local |

## 📞 Support

- Check logs: `docker-compose logs [service]`
- Health check: `curl http://localhost:8080/bytebite/health`
- Test API: See DEPLOYMENT.md for curl examples

---

**Last Updated**: May 2026  
**Version**: 1.0.0  
**Status**: Production Ready ✅
