# ByteBite Project Context — CLAUDE.md

**Last Updated**: June 2026 | **Status**: Learning Lab ✓

## 🎯 Project Purpose

ByteBite is a **hands-on troubleshooting laboratory** for an advanced Java debugging book. It teaches senior engineers to diagnose production failures through log analysis, not a production system. Readers trigger engineered failures on specific endpoints and analyze `catalina.2026-06-01.log` (daily logs) to identify root causes.

**Author Role**: CTO/Solution Architect — prefers architecture-first, concise communication; no full boilerplate unless explicitly requested.

---

## 📐 Architecture & Stack

### Immutable Stack
- **JDK**: Temurin JDK 21 (in Docker)
- **App Server**: Apache Tomcat 11
- **Database**: PostgreSQL 15-alpine with HikariCP
- **LDAP**: OpenLDAP 1.5.0 (Tomcat Realm authentication)
- **Build**: Maven 3.6+
- **Orchestration**: Docker Compose 3.8 on `bytebite-net` network
- **Logging**: SLF4J/JUL → catalina.log (daily rotation)

### Core Components

```
Tomcat 11 (JDK 21)
├── 6 Servlets (LoginServlet, MenuServlet, CheckoutServlet, AnalyticsServlet, KDSServlet, HealthServlet)
├── 4 Filters (RequestLoggingFilter, LDAPAuthenticationFilter, CORSFilter, SecurityHeadersFilter)
├── 3 Services (UserService, OrderService, AnalyticsService)
└── FailureInjectionUtil (5 togglable failure vectors)

PostgreSQL 15-alpine
├── 11 tables (users, orders, analytics_events, kds_orders, etc.)
└── HikariCP pool (max 20 connections)

OpenLDAP 1.5.0
├── Base DN: dc=bytebite,dc=local
├── 6 test users (admin, manager, staff1, staff2, kds_operator, customer1)
└── Tomcat JNDI Realm integration
```

---

## 🔴 5 Engineered Failure Vectors

**Trigger all failures via HTTP request parameter**: `?failureType=<type>`

| Endpoint | Failure Type | HTTP Trigger | Root Cause | What to Look For |
|---|---|---|---|---|
| `/api/menu` | `db_leak` | `GET /api/menu?failureType=db_leak` | Connection not closed; HikariCP pool exhaustion | "⚠️ Connection leak scenario" in catalina.log |
| `/login` | `ldap_timeout` | `POST /login?failureType=ldap_timeout` + creds | LDAP directory unavailable/timeout | "⚠️ LDAP Failure Injection: Simulating connection timeout" |
| `/api/analytics?action=all-time-report` | `oom` | `GET /api/analytics?action=all-time-report&failureType=oom` | Unbounded query loads all events into memory | "⚠️ OOM FAILURE INJECTION" + heap spikes |
| `/api/kds` | `deadlock` | `POST /api/kds` JSON `{"failureType":"deadlock"}` | Inverted lock ordering (order→inventory vs. inventory→order) | "❌ DEADLOCK SCENARIO: Both threads are now BLOCKED" |
| `/checkout` | `ssl` | `POST /checkout` JSON `{"failureType":"ssl"}` | Self-signed certificate validation fails | "❌ SSL Certificate Validation Failed" |

**Authentication required for KDS & Checkout**: Pass session cookie or login first.

---

## 📂 Key Files & Locations

### Servlets (Failure Injection Points)
- `src/main/java/com/bytebite/servlet/MenuServlet.java:79` — DB leak injection
- `src/main/java/com/bytebite/servlet/LoginServlet.java:88` — LDAP timeout injection
- `src/main/java/com/bytebite/servlet/AnalyticsServlet.java:96` — OOM injection
- `src/main/java/com/bytebite/servlet/KDSServlet.java:55` — Deadlock injection
- `src/main/java/com/bytebite/servlet/CheckoutServlet.java:68` — SSL injection

### Utilities
- `src/main/java/com/bytebite/util/FailureInjectionUtil.java` — Central failure dispatcher
- `src/main/java/com/bytebite/util/Constants.java` — App constants
- `src/main/java/com/bytebite/filter/LDAPAuthenticationFilter.java:50` — Allows unauthenticated failure test requests

### Configuration
- `docker-compose.yml` — Service orchestration
- `docker/tomcat/logging.properties` — JUL handler config (writes to catalina.YYYY-MM-DD.log)
- `src/main/webapp/WEB-INF/web.xml` — Servlet mappings, context params
- `docker/postgres/init.sql` — Database schema (11 tables)

### Documentation
- `README.md` — Architecture, quick start, failure vectors
- `DEPLOYMENT.md` — Production deployment, Kubernetes, scaling
- `TESTING.md` — Unit/integration/E2E test guide
- `memory/MEMORY.md` — Session memory index

---

## 🚀 Quick Reference

### Build & Deploy
```bash
mvn clean package -DskipTests              # Build WAR
docker-compose down -v                     # Clean volumes
docker-compose up -d                       # Start all services
sleep 45 && docker-compose ps              # Verify health
```

### Test Failures
```bash
# DB leak (no auth required)
curl "http://localhost:8080/bytebite/api/menu?failureType=db_leak"

# LDAP timeout (no auth required)
curl -X POST "http://localhost:8080/bytebite/login?failureType=ldap_timeout" \
  -d "username=admin&password=admin"

# OOM (no auth required)
curl "http://localhost:8080/bytebite/api/analytics?action=all-time-report&failureType=oom"

# With auth (for KDS/Checkout)
curl -c /tmp/cookies.txt -X POST "http://localhost:8080/bytebite/login" \
  -d "username=admin&password=admin"
curl -b /tmp/cookies.txt -X POST "http://localhost:8080/bytebite/api/kds" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-1", "status":"IN_PROGRESS", "failureType":"deadlock"}'
```

### Check Logs
```bash
docker exec bytebite-tomcat tail -f /usr/local/tomcat/logs/catalina.2026-06-01.log | \
  grep -E "FAILURE|DEADLOCK|Connection leak|OOM|SSL"
```

### Service URLs
- **App**: http://localhost:8080/bytebite/login (admin/admin)
- **Health**: http://localhost:8080/bytebite/health
- **PostgreSQL**: localhost:5432 (bytebite/bytebite_secure_pwd_123)
- **LDAP**: localhost:389 (cn=admin,dc=bytebite,dc=local / admin_secure_pwd_123)

---

## 🔧 Development Guidelines

### Code Style
- **Architecture first**: Lead with structural design, then implementation snippets
- **No boilerplate**: Snippets over full file generation unless explicitly requested
- **Minimal comments**: Only WHY, not WHAT (good naming self-documents)
- **No premature abstractions**: Three similar lines is OK
- **No error handling for impossible scenarios**: Trust framework guarantees

### When Adding Features
1. **Servlet layer**: Add in `src/main/java/com/bytebite/servlet/`
2. **Service layer**: Add in `src/main/java/com/bytebite/service/`
3. **Database**: Update `docker/postgres/init.sql` schema
4. **Frontend**: Add HTML/CSS/JS in `src/main/webapp/`
5. **Failure injection**: Use `FailureInjectionUtil` in servlet, add constant to `FAILURE_*` family

### Database Queries
- All use **PreparedStatements** (SQL injection prevention)
- JNDI lookup: `ctx.lookup("java:comp/env/jdbc/bytebite")`
- Connection pooling: HikariCP managed by Tomcat

### Testing
- Unit tests: `src/test/java/com/bytebite/util/` (SecurityUtilTest, PaginationUtilTest, DateTimeUtilTest)
- Run: `mvn test`
- Integration: Test via `docker-compose` + curl

---

## 🐳 Docker Notes

### Volumes
- `postgres_data` → `/var/lib/postgresql/data`
- `openldap_data` → `/var/lib/ldap`
- `openldap_config` → `/etc/ldap/slapd.d`
- `tomcat_logs` → `/usr/local/tomcat/logs`

### Health Checks
```yaml
postgres: pg_isready (10s interval)
openldap: ldapsearch query (10s interval)
tomcat: curl /bytebite/ (15s interval, 30s start delay)
```

### Logging
- **Handler**: org.apache.juli.AsyncFileHandler (JUL, not Logback)
- **Output**: `${CATALINA_BASE}/logs/catalina.YYYY-MM-DD.log`
- **Rotation**: Daily, 90-day retention
- **Packages logged**: `com.bytebite.*` (ALL level)

---

## ✅ Known Gotchas & Testing Notes

### Failure Injection Requirements
- **No global on/off switch**: Failures activate only when HTTP parameter is passed
- **Auth bypass for tests**: LDAPAuthenticationFilter permits requests with `?failureType` param (line 50)
- **KDS & Checkout need session**: These endpoints check auth first; pass session cookie from login

### Database State
- Schema initialized by `docker/postgres/init.sql` at container startup
- `adhoc` container runs post-init tasks (one-time)
- Menu items are hardcoded fallback if DB query fails

### Logging Pipeline
1. SLF4J captures all `logger.info/warn/error` calls
2. JUL handler writes to async file handler
3. Catalina.log receives daily rotation (e.g., `catalina.2026-06-01.log`)
4. Search logs: grep for "FAILURE INJECTION ACTIVATED" or specific failure type

### Timestamps in Logs
- Format: `DD-MMM-YYYY HH:MM:SS.ms` (e.g., `01-Jun-2026 10:03:32.423`)
- All logged in UTC (Tomcat default)

---

## 📝 Common Tasks

### Rebuild After Code Changes
```bash
mvn clean package -DskipTests
docker-compose restart tomcat
```

### Check Specific Failure in Logs
```bash
docker exec bytebite-tomcat grep "FAILURE INJECTION ACTIVATED: db_leak" \
  /usr/local/tomcat/logs/catalina.*.log
```

### Inspect Database Schema
```bash
docker exec bytebite-postgres psql -U bytebite -d bytebite_db -c "\dt bytebite.*"
```

### View LDAP Tree
```bash
docker exec bytebite-openldap ldapsearch -x -b "dc=bytebite,dc=local"
```

### Reset Everything
```bash
docker-compose down -v
mvn clean
docker-compose up -d
sleep 45
```

---

## 🎓 For the Book

**Chapter Focus**: ByteBite demonstrates real production failure patterns through controlled labs:
- **DB Connection Pools**: Show HikariCP exhaustion in logs
- **LDAP Integration**: Showcase auth timeout signatures
- **Heap Pressure**: Unbounded queries triggering OOM
- **Thread Contention**: Deadlock detection via thread dumps
- **SSL/TLS**: Certificate validation failures

Each failure leaves diagnostic breadcrumbs in catalina.log for readers to trace.

---

## 📚 References

- **Memory**: See `memory/MEMORY.md` for session-specific context
- **Testing**: `TESTING.md` — unit, integration, load, security tests
- **Deployment**: `DEPLOYMENT.md` — production hardening, scaling, backups
- **API**: `README.md` — endpoint reference, default credentials

---

**Generated**: June 2026 | Maintained by: CTO/Solution Architect