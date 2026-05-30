# ByteBite Deployment & Testing Guide

## Prerequisites

- Docker & Docker Compose installed
- Java 11+ (for local testing)
- Maven 3.6+ (for building)
- Git

## Quick Start (Docker)

### 1. Build the Docker Image

```bash
cd d:\workspace\ai\code\log-analysis-ai-usecase-app
docker build -t bytebite-tomcat:latest ./docker/tomcat
```

### 2. Start Services

```bash
# Start all services (PostgreSQL, OpenLDAP, Tomcat)
docker-compose up -d

# View logs
docker-compose logs -f

# Check service status
docker-compose ps
```

### 3. Access the Application

- **UI**: http://localhost:8080/bytebite/login
- **Health Check**: http://localhost:8080/bytebite/health
- **HTTPS**: https://localhost:8443/bytebite/login (if SSL configured)

### 4. Test Credentials

- **Username**: admin
- **Password**: admin

---

## Building Locally (With Maven)

### 1. Compile & Package

```bash
mvn clean compile
mvn package -DskipTests
```

### 2. Run Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=SecurityUtilTest

# Run with coverage
mvn test jacoco:report
```

### 3. View Build Artifacts

```bash
# WAR file location
target/bytebite.war

# Place in Tomcat
docker cp target/bytebite.war bytebite-tomcat:/usr/local/tomcat/webapps/
```

---

## Testing

### Unit Tests

Test files are in `src/test/java/com/bytebite/util/`:

- **SecurityUtilTest**: Password hashing, input sanitization, email validation
- **PaginationUtilTest**: Pagination logic, boundary conditions
- **DateTimeUtilTest**: Date/time operations, formatting

```bash
mvn test
```

### Integration Tests

Test the application with curl:

```bash
# Test health endpoint
curl http://localhost:8080/bytebite/health

# Test login
curl -X POST http://localhost:8080/bytebite/login \
  -d "username=admin&password=admin" \
  -H "Content-Type: application/x-www-form-urlencoded"

# Test menu API
curl http://localhost:8080/bytebite/api/menu

# Test analytics
curl -X POST http://localhost:8080/bytebite/api/analytics \
  -d '{"eventType":"TEST","data":"test"}' \
  -H "Content-Type: application/json"
```

### Frontend Testing

1. Open browser to http://localhost:8080/bytebite/login
2. Log in with admin/admin
3. Navigate through:
   - **Menu**: Browse and add items to cart
   - **Checkout**: Complete order form
   - **Analytics**: View event stats
   - **KDS**: Kitchen Display System

---

## Database Setup

### Access PostgreSQL

```bash
# Connect to PostgreSQL container
docker exec -it bytebite-postgres psql -U bytebite -d bytebite_db

# List tables
\dt bytebite.*

# View sample data
SELECT * FROM bytebite.users;
SELECT * FROM bytebite.analytics_events LIMIT 10;
```

### LDAP Setup

```bash
# Access LDAP container
docker exec -it bytebite-openldap ldapsearch -x -b "dc=bytebite,dc=local"

# Test LDAP user
docker exec -it bytebite-openldap ldapwhoami -D "uid=admin,ou=people,dc=bytebite,dc=local" -w "admin_secure_pwd_123"
```

---

## Deployment Checklist

- [ ] Docker Compose up and running
- [ ] All 3 services healthy (postgres, openldap, tomcat)
- [ ] Database initialized with schema
- [ ] LDAP directory populated
- [ ] Tomcat deployed WAR file
- [ ] Login works with test credentials
- [ ] Menu loads correctly
- [ ] Checkout form functional
- [ ] Analytics events captured
- [ ] KDS displays orders
- [ ] Health check returns UP status

---

## Troubleshooting

### Services Won't Start

```bash
# Check logs
docker-compose logs postgres
docker-compose logs openldap
docker-compose logs tomcat

# Restart services
docker-compose restart

# Full reset
docker-compose down -v
docker-compose up -d
```

### Database Connection Issues

```bash
# Test database connectivity
docker exec bytebite-tomcat curl -s http://localhost:8080/bytebite/health | jq .

# Check if database is ready
docker exec bytebite-postgres pg_isready -U bytebite
```

### LDAP Authentication Failures

```bash
# Verify LDAP is running
docker exec bytebite-openldap ldapsearch -x -b "dc=bytebite,dc=local" | head -20

# Check LDAP logs
docker-compose logs openldap | tail -20
```

### Memory Issues

```bash
# Increase JVM memory
# Edit docker-compose.yml:
# JAVA_OPTS: "-Xmx1024m -Xms512m ..."

# Restart Tomcat
docker-compose restart tomcat
```

---

## Production Considerations

### Security

1. **Change default passwords**
   - PostgreSQL: Update `DB_PASSWORD` in `.env`
   - OpenLDAP: Update `LDAP_ADMIN_PASSWORD`
   - Tomcat: Generate new SSL keystore

2. **Enable HTTPS**
   - Update `server.xml` with valid SSL certificate
   - Use environment variables for cert paths

3. **Database backups**
   ```bash
   docker exec bytebite-postgres pg_dump -U bytebite bytebite_db > backup.sql
   ```

### Monitoring

1. **Health Checks**
   ```bash
   # Monitor health endpoint
   watch -n 5 'curl -s http://localhost:8080/bytebite/health | jq .'
   ```

2. **Logs**
   ```bash
   docker-compose logs -f --tail=100 tomcat
   ```

3. **Metrics**
   - Database connection pool stats
   - Request latency
   - Error rates

### Scaling

1. **Multiple Tomcat instances**
   - Use load balancer (nginx, haproxy)
   - Point to same database

2. **Database tuning**
   - Adjust connection pool size
   - Add indexes on frequently queried columns
   - Enable query logging

---

## API Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/login` | POST | User authentication |
| `/logout` | GET | Clear session |
| `/menu` | GET | Display menu |
| `/api/menu` | GET | Menu data (JSON) |
| `/checkout` | GET/POST | Checkout page & order creation |
| `/api/orders` | GET/POST | Orders API |
| `/analytics` | GET | Analytics dashboard |
| `/api/analytics` | POST | Event tracking |
| `/kds` | GET | Kitchen display system |
| `/api/kds` | GET/POST | KDS API |
| `/health` | GET | Health check |
| `/status` | GET | Application status |

---

## File Structure

```
ByteBite/
├── docker/
│   ├── tomcat/
│   │   ├── Dockerfile
│   │   ├── server.xml
│   │   └── context.xml
│   ├── postgres/
│   │   └── init.sql
│   └── openldap/
│       └── bootstrap.ldif
├── src/
│   ├── main/
│   │   ├── java/com/bytebite/
│   │   │   ├── config/ (Listeners, configurations)
│   │   │   ├── filter/ (Request filters)
│   │   │   ├── servlet/ (HTTP request handlers)
│   │   │   ├── service/ (Business logic)
│   │   │   └── util/ (Utilities)
│   │   └── webapp/
│   │       ├── css/ (Stylesheets)
│   │       ├── js/ (JavaScript)
│   │       ├── login/ (Login forms)
│   │       └── *.html (Pages)
│   └── test/
│       └── java/com/bytebite/
│           └── util/ (Unit tests)
├── pom.xml (Maven configuration)
├── docker-compose.yml (Container orchestration)
└── README.md
```

---

## Next Steps

1. **Customize branding** - Update CSS colors, logos
2. **Implement LDAP auth** - Replace mock authentication
3. **Add database queries** - Implement actual service logic
4. **Deploy to cloud** - AWS, Azure, GCP, Kubernetes
5. **Set up monitoring** - Prometheus, Grafana, ELK stack
6. **Performance testing** - Load testing with JMeter, Gatling
