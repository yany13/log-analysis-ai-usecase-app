# ByteBite Technology Stack Specification

## Core Runtime & Application Server

**Java Runtime:** Eclipse Temurin JDK 21 (LTS)
- Long-term support release
- Used in Docker container: `tomcat:11-jdk21-temurin`
- Provides full Jakarta Servlet API 4.0.1 support

**App Server:** Apache Tomcat 11
- Latest stable release
- HTTP Port: 8080
- HTTPS Port: 8443 (SSL configuration ready)
- AJP Port: 8009
- Volume mounting: `/usr/local/tomcat/logs` for log access

## Data & Directory Services

**Database:** PostgreSQL 15-alpine
- Docker image: `postgres:15-alpine`
- Port: 5432 (internal to docker network)
- Connection pooling: HikariCP 5.0.1 (20 max connections)
- Schema: `bytebite` (11 tables with indexes)
- Credentials: `bytebite` / `bytebite_secure_pwd_123`
- Volume: `postgres_data` for persistence

**Directory Service:** OpenLDAP 1.5.0
- Docker image: `osixia/openldap:1.5.0`
- Port: 389 (LDAP)
- Port: 636 (LDAPS - TLS)
- Base DN: `dc=bytebite,dc=local`
- Admin: `cn=admin,dc=bytebite,dc=local` / `admin_secure_pwd_123`
- Test Users: 6 (admin, manager, staff1, staff2, kds_operator, customer1)

## Build & Dependency Management

**Build Tool:** Apache Maven 3.6+
- Build command: `mvn clean package`
- Output: `target/bytebite.war`
- Configuration: `pom.xml`

**Key Dependencies:**
- PostgreSQL JDBC Driver: 42.7.1
- Jackson JSON: 2.15.2
- HikariCP Connection Pool: 5.0.1
- SLF4J Logging: 2.0.9
- Logback: 1.4.12
- Jakarta Servlet API: 4.0.1
- JUnit 4: 4.13.2
- Mockito: 5.4.1

## Frontend Stack

- **HTML:** HTML5 with Jakarta Servlet rendering
- **CSS:** Vanilla CSS3 (Flexbox, Grid) via `/css/styles.css`
- **JavaScript:** Vanilla ES6+ (no frameworks)
- **HTTP Client:** Fetch API for XHR/CORS requests

## Container Orchestration

**Docker Compose:** Version 3.8+
- Network: `bytebite-net` (bridge)
- Services: 3 (tomcat, postgres, openldap)
- Volume persistence: postgres_data, openldap_data, openldap_config, tomcat_logs

## Logging & Monitoring

**Log Output Target:** `/usr/local/tomcat/logs/catalina.out`
- Standard Java logging patterns
- Structured format for analysis
- Request/response logging with timing
- Error stack traces for troubleshooting

**Health Monitoring:** Health endpoint at `/bytebite/health`
- Database connectivity check
- LDAP directory check
- Service status reporting

## Performance Configuration

**Connection Pool (HikariCP):**
- Max connections: 20
- Min idle: 5
- Connection timeout: 30 seconds
- Idle timeout: 10 minutes
- Max lifetime: 30 minutes

**JVM Memory:**
- Default: `-Xmx512m -Xms256m`
- Configurable via `JAVA_OPTS` in docker-compose.yml

**Event Batch Processing:**
- Batch size: 100 events
- Batch timeout: 60 seconds
- Async execution with ExecutorService

## Security Framework

- Session: HttpOnly, Secure, SameSite=Strict cookies
- Password Hashing: SHA-256 with Base64 encoding
- Input Sanitization: HTML entity encoding
- SQL Injection Prevention: Prepared statements
- CSRF Protection: Token generation utility available
- Security Headers: CSP, HSTS, X-Frame-Options, etc.

---

**Last Updated:** May 2026  
**Status:** Production Specification
