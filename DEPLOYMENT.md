# ByteBite Deployment Guide

Comprehensive guide for deploying ByteBite to development, staging, and production environments.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Development Deployment](#development-deployment)
3. [Production Deployment](#production-deployment)
4. [Configuration Management](#configuration-management)
5. [SSL/TLS Setup](#ssltls-setup)
6. [Database Management](#database-management)
7. [Backup & Recovery](#backup--recovery)
8. [Monitoring & Alerting](#monitoring--alerting)
9. [Troubleshooting](#troubleshooting)
10. [Scaling & Performance](#scaling--performance)

## Quick Start

Get ByteBite running in 2 minutes:

```bash
cd D:\workspace\ai\code\log-analysis-ai-usecase-app
docker-compose up -d
# Wait 30-45 seconds for initialization
# Open http://localhost:8080/bytebite/login
# Username: admin | Password: admin
```

## Development Deployment

### Local Docker Deployment

For development and testing with full containerization:

#### Prerequisites
- Docker Desktop (with Docker Compose)
- 2GB available RAM minimum
- 10GB disk space

#### Deployment Steps

```bash
# 1. Navigate to project
cd D:\workspace\ai\code\log-analysis-ai-usecase-app

# 2. Build the WAR file
mvn clean package -DskipTests

# 3. Start all services
docker-compose up -d

# 4. Verify services are running
docker-compose ps

# 5. Check application health
curl http://localhost:8080/bytebite/health

# 6. Access application
# Browser: http://localhost:8080/bytebite/login
```

#### Service Status Verification

```bash
# Check all containers
docker-compose ps

# Expected output:
# NAME                    STATUS
# bytebite-postgres       Up 2min (healthy)
# bytebite-openldap       Up 2min (healthy)
# bytebite-adhoc          Exit 0
# bytebite-tomcat         Up 2min (healthy)

# View logs
docker-compose logs -f tomcat    # Tomcat logs
docker-compose logs postgres     # PostgreSQL logs
docker-compose logs openldap     # OpenLDAP logs
```

#### Stop & Cleanup

```bash
# Stop all services (keeps volumes)
docker-compose stop

# Stop and remove containers (keeps volumes)
docker-compose down

# Complete cleanup (removes volumes - DESTRUCTIVE)
docker-compose down -v
```

### Local Development (No Docker)

For Java-native development with external services:

#### Prerequisites
- Java 11+
- Maven 3.6+
- PostgreSQL 15+ (external)
- OpenLDAP (external)

#### Setup

```bash
# 1. Start PostgreSQL (external)
# Connection: localhost:5432
# Database: bytebite_db
# User: bytebite
# Password: bytebite_secure_pwd_123

# 2. Start OpenLDAP (external)
# Connection: localhost:389
# Base DN: dc=bytebite,dc=local
# Admin: cn=admin,dc=bytebite,dc=local
# Password: admin_secure_pwd_123

# 3. Initialize database schema
psql -h localhost -U bytebite -d bytebite_db -f docker/postgres/init.sql

# 4. Initialize LDAP
# Use ldapadd or similar tool to import bootstrap.ldif

# 5. Build and run
mvn clean package
# Deploy bytebite.war to Tomcat or run with embedded server
```

## Production Deployment

### Kubernetes Deployment

For production environments using Kubernetes (recommended for scale):

#### Prerequisite: Helm Chart

Create `helm/bytebite/Chart.yaml`:

```yaml
apiVersion: v2
name: bytebite
description: ByteBite Restaurant Order Management System
type: application
version: 1.0.0
appVersion: "1.0.0"
```

#### Deployment Steps

```bash
# 1. Build and push Docker image to registry
docker build -t my-registry/bytebite:1.0.0 -f docker/tomcat/Dockerfile .
docker push my-registry/bytebite:1.0.0

# 2. Create namespace
kubectl create namespace bytebite-prod

# 3. Deploy with Helm
helm install bytebite ./helm/bytebite \
  -n bytebite-prod \
  --set image.repository=my-registry/bytebite \
  --set image.tag=1.0.0 \
  --set replicas=3 \
  --set ssl.enabled=true

# 4. Verify deployment
kubectl -n bytebite-prod get pods
kubectl -n bytebite-prod get svc

# 5. Access application
# Via Ingress or LoadBalancer endpoint
```

### Docker Compose Production Stack

For smaller deployments (up to 10,000 users):

#### Pre-deployment Checklist

- [ ] SSL certificates ready
- [ ] Database backups configured
- [ ] Environment variables set
- [ ] Secrets managed (use .env file)
- [ ] Monitoring stack ready
- [ ] Load balancer configured
- [ ] Reverse proxy (nginx) ready

#### Secure Deployment

```bash
# 1. Create .env file with production secrets (NEVER commit this)
cat > .env << EOF
POSTGRES_PASSWORD=<STRONG_DB_PASSWORD>
LDAP_ADMIN_PASSWORD=<STRONG_LDAP_PASSWORD>
SSL_KEYSTORE_PASSWORD=<STRONG_KEYSTORE_PASSWORD>
DB_POOL_SIZE=50
JAVA_MEMORY=2048m
EOF

# 2. Update docker-compose.yml for production
# - Set replicas for Tomcat
# - Configure SSL paths
# - Set resource limits
# - Enable monitoring

# 3. Deploy
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# 4. Verify
docker-compose ps
curl https://your-domain.com/bytebite/health
```

### Reverse Proxy Configuration (Nginx)

```nginx
upstream bytebite_backend {
    server bytebite-tomcat:8080;
    keepalive 32;
}

server {
    listen 80;
    server_name bytebite.example.com;
    
    # Redirect HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name bytebite.example.com;
    
    # SSL certificates
    ssl_certificate /etc/ssl/certs/bytebite.crt;
    ssl_certificate_key /etc/ssl/private/bytebite.key;
    
    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "DENY" always;
    
    # Proxy configuration
    location /bytebite/ {
        proxy_pass http://bytebite_backend;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
    
    # Static assets caching
    location ~* \.(js|css|png|jpg|gif|ico)$ {
        proxy_cache_valid 200 1d;
        expires 1d;
    }
}
```

## Configuration Management

### Environment Variables

All configuration via environment variables in `docker-compose.yml`:

#### Database Configuration
```yaml
DB_HOST: postgres                          # PostgreSQL hostname
DB_PORT: 5432                             # PostgreSQL port
DB_NAME: bytebite_db                      # Database name
DB_USER: bytebite                         # Database user
DB_PASSWORD: bytebite_secure_pwd_123      # Database password
```

#### LDAP Configuration
```yaml
LDAP_HOST: openldap                       # LDAP server hostname
LDAP_PORT: 389                            # LDAP port
LDAP_BASE_DN: dc=bytebite,dc=local       # Base distinguished name
LDAP_BIND_DN: cn=admin,dc=bytebite,dc=local  # Admin bind DN
LDAP_BIND_PASSWORD: admin_secure_pwd_123 # Admin password
```

#### SSL/TLS Configuration
```yaml
SSL_ENABLED: "true"                       # Enable HTTPS
SSL_KEYSTORE_PATH: /path/to/keystore.jks # Keystore location
SSL_KEYSTORE_PASSWORD: pwd123             # Keystore password
```

#### Application Configuration
```yaml
LOG_LEVEL: INFO                           # Logging level (DEBUG, INFO, WARN, ERROR)
FAILURE_INJECTION_ENABLED: "false"        # Chaos testing (disable in prod)
JAVA_MEMORY: 1024m                        # JVM heap size
```

### Secrets Management

#### Option 1: Environment File (.env)

```bash
# Create .env (add to .gitignore)
cat > .env << EOF
POSTGRES_PASSWORD=your_secure_password
LDAP_ADMIN_PASSWORD=your_secure_password
SSL_KEYSTORE_PASSWORD=your_secure_password
EOF

# Use in docker-compose
docker-compose --env-file .env up -d
```

#### Option 2: Docker Secrets (Swarm/K8s)

```bash
# Create secrets
echo "secure_password" | docker secret create db_password -
echo "secure_password" | docker secret create ldap_password -

# Reference in compose file
services:
  postgres:
    environment:
      POSTGRES_PASSWORD_FILE: /run/secrets/db_password
    secrets:
      - db_password
```

#### Option 3: HashiCorp Vault

```bash
# Store secrets in Vault
vault kv put secret/bytebite/db password="xxx"
vault kv put secret/bytebite/ldap password="xxx"

# Retrieve in startup script
export DB_PASSWORD=$(vault kv get -field=password secret/bytebite/db)
export LDAP_PASSWORD=$(vault kv get -field=password secret/bytebite/ldap)
```

## SSL/TLS Setup

### Generate Self-Signed Certificate (Development)

```bash
# Create self-signed cert valid for 365 days
keytool -genkey -alias tomcat -keyalg RSA -keysize 2048 -validity 365 \
  -keystore keystore.jks -storepass changeit \
  -dname "CN=localhost,OU=IT,O=ByteBite,L=City,ST=State,C=US"

# Place in docker/tomcat/
cp keystore.jks docker/tomcat/keystore.jks
```

### Use CA-Signed Certificate (Production)

```bash
# 1. Generate certificate signing request (CSR)
keytool -certreq -alias tomcat -keyalg RSA -keysize 2048 \
  -keystore keystore.jks -storepass changeit -file bytebite.csr

# 2. Submit CSR to Certificate Authority

# 3. Import signed certificate chain
keytool -import -alias tomcat -trustcacerts \
  -file signed_certificate.crt \
  -keystore keystore.jks -storepass changeit

# 4. Verify certificate
keytool -list -v -keystore keystore.jks -storepass changeit

# 5. Place in docker/tomcat/keystore.jks
```

### Configure Tomcat for HTTPS

In `docker/tomcat/server.xml`:

```xml
<Connector port="8443" protocol="HTTP/1.1"
           scheme="https" secure="true"
           SSLEnabled="true"
           keystoreFile="/usr/local/tomcat/conf/keystore.jks"
           keystorePass="${SSL_KEYSTORE_PASSWORD}"
           keyAlias="tomcat"
           clientAuth="false"
           sslProtocol="TLSv1.2,TLSv1.3">
</Connector>
```

## Database Management

### Database Initialization

First-time setup:

```bash
# 1. PostgreSQL initializes from docker/postgres/init.sql
# 2. Adhoc container runs post-initialization tasks
# 3. Schema, tables, and test data are created

# Verify initialization
docker exec bytebite-postgres psql -U bytebite -d bytebite_db -c "\dt bytebite.*"
```

### Database Backup

#### Automated Backup Script

```bash
#!/bin/bash
# backup-database.sh

BACKUP_DIR="/backups/bytebite"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/bytebite_$TIMESTAMP.sql.gz"

# Create backup directory
mkdir -p $BACKUP_DIR

# Perform backup
docker exec bytebite-postgres pg_dump -U bytebite bytebite_db | gzip > $BACKUP_FILE

# Keep only last 30 days
find $BACKUP_DIR -name "bytebite_*.sql.gz" -mtime +30 -delete

echo "Backup created: $BACKUP_FILE"
```

#### Scheduled Backups (Cron)

```bash
# Add to crontab (daily at 2 AM)
0 2 * * * /path/to/backup-database.sh >> /var/log/bytebite-backup.log 2>&1

# View cron jobs
crontab -l
```

### Database Restore

```bash
# 1. Stop Tomcat
docker-compose stop tomcat

# 2. Restore database
gunzip < /backups/bytebite/bytebite_20260601_020000.sql.gz | \
  docker exec -i bytebite-postgres psql -U bytebite bytebite_db

# 3. Restart Tomcat
docker-compose up -d tomcat

# 4. Verify
curl http://localhost:8080/bytebite/health
```

## Backup & Recovery

### Full System Backup

```bash
# Backup application and data
backup-system.sh << 'EOF'
#!/bin/bash

BACKUP_DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups/bytebite_$BACKUP_DATE"

mkdir -p $BACKUP_DIR

# Backup application
cp -r target/bytebite.war $BACKUP_DIR/

# Backup configuration
cp docker-compose.yml $BACKUP_DIR/
cp .env $BACKUP_DIR/.env.backup  # If using .env

# Backup database volume
docker run --rm -v postgres_data:/data -v $BACKUP_DIR:/backup \
  alpine tar czf /backup/postgres_data.tar.gz -C /data .

# Backup LDAP volume
docker run --rm -v openldap_data:/data -v $BACKUP_DIR:/backup \
  alpine tar czf /backup/openldap_data.tar.gz -C /data .

echo "Full backup completed: $BACKUP_DIR"
EOF
```

### Disaster Recovery

```bash
# 1. Stop all services
docker-compose down

# 2. Remove volumes (destructive)
docker volume rm postgres_data openldap_data openldap_config tomcat_logs

# 3. Restore from backup
BACKUP_DIR="/backups/bytebite_20260601_020000"

# Restore database
docker volume create postgres_data
docker run --rm -v postgres_data:/data -v $BACKUP_DIR:/backup \
  alpine tar xzf /backup/postgres_data.tar.gz -C /data

# Restore LDAP
docker volume create openldap_data
docker run --rm -v openldap_data:/data -v $BACKUP_DIR:/backup \
  alpine tar xzf /backup/openldap_data.tar.gz -C /data

# 4. Restart services
docker-compose up -d

# 5. Verify
curl http://localhost:8080/bytebite/health
```

## Monitoring & Alerting

### Container Health Monitoring

```bash
# Check container health status
docker-compose ps

# Monitor in real-time
docker stats

# Get detailed container logs
docker logs -f bytebite-tomcat
docker logs -f bytebite-postgres
docker logs -f bytebite-openldap
```

### Application Health Endpoint

```bash
# Health check
curl -v http://localhost:8080/bytebite/health

# Expected response:
# {
#   "status": "UP",
#   "timestamp": 1622529780,
#   "service": "ByteBite",
#   "version": "1.0.0",
#   "database": { "status": "UP" },
#   "ldap": { "status": "UP" }
# }
```

### Prometheus Integration

Add to `docker-compose.yml`:

```yaml
prometheus:
  image: prom/prometheus:latest
  ports:
    - "9090:9090"
  volumes:
    - ./prometheus.yml:/etc/prometheus/prometheus.yml
    - prometheus_data:/prometheus
  command:
    - '--config.file=/etc/prometheus/prometheus.yml'
```

### Grafana Dashboards

```bash
# Start Grafana
docker run -d -p 3000:3000 \
  -e GF_SECURITY_ADMIN_PASSWORD=admin \
  grafana/grafana:latest

# Access at http://localhost:3000
# Default: admin/admin
# Add Prometheus data source: http://prometheus:9090
```

## Troubleshooting

### Common Issues

#### Port Already in Use

```bash
# Find process using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Or change port in docker-compose.yml
ports:
  - "8888:8080"  # Change to 8888
```

#### Services Won't Start

```bash
# Check logs
docker-compose logs

# Common fixes
docker-compose down -v
docker system prune -a
docker-compose up -d --build
```

#### Database Connection Issues

```bash
# Check PostgreSQL
docker exec bytebite-postgres pg_isready

# Check connection
docker exec bytebite-postgres psql -U bytebite -c "SELECT version();"

# View PostgreSQL logs
docker logs bytebite-postgres
```

#### LDAP Authentication Failures

```bash
# Test LDAP connection
docker exec bytebite-openldap ldapsearch -x -b dc=bytebite,dc=local

# Check LDAP logs
docker logs bytebite-openldap

# Verify users exist
docker exec bytebite-openldap ldapsearch -x -b "ou=users,dc=bytebite,dc=local"
```

#### Application Startup Issues

```bash
# Monitor startup logs
docker-compose logs -f tomcat

# Wait for full initialization (30-45 seconds)

# Check health endpoint
curl http://localhost:8080/bytebite/health

# View Tomcat logs
docker exec bytebite-tomcat tail -f logs/catalina.out
```

## Scaling & Performance

### Horizontal Scaling with Tomcat

```yaml
# docker-compose.yml - Multiple Tomcat instances
version: '3.8'

services:
  tomcat1:
    build: ./docker/tomcat
    ports:
      - "8080:8080"
    environment:
      <<: *env_variables
  
  tomcat2:
    build: ./docker/tomcat
    ports:
      - "8081:8080"
    environment:
      <<: *env_variables
  
  tomcat3:
    build: ./docker/tomcat
    ports:
      - "8082:8080"
    environment:
      <<: *env_variables
```

### Load Balancing Configuration (Nginx)

```nginx
upstream bytebite_backend {
    server tomcat1:8080 weight=3;
    server tomcat2:8080 weight=2;
    server tomcat3:8080 weight=1;
    
    keepalive 32;
}

server {
    listen 80;
    location /bytebite/ {
        proxy_pass http://bytebite_backend;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
    }
}
```

### Connection Pool Optimization

Adjust in `docker-compose.yml`:

```yaml
environment:
  JDBC_HIKARI_MAX_POOL_SIZE: 50      # Max connections
  JDBC_HIKARI_MIN_IDLE: 10           # Min idle
  JDBC_HIKARI_CONNECTION_TIMEOUT: 30  # Timeout (seconds)
  JDBC_HIKARI_IDLE_TIMEOUT: 600      # Idle timeout (seconds)
```

### JVM Tuning

```yaml
environment:
  JAVA_OPTS: >
    -Xms2048m 
    -Xmx2048m 
    -XX:+UseG1GC 
    -XX:MaxGCPauseMillis=200 
    -XX:+ParallelRefProcEnabled
```

### Database Query Optimization

Monitor slow queries:

```sql
-- Enable slow query log
ALTER SYSTEM SET log_min_duration_statement = 1000;  -- Log queries > 1s

-- View slow queries
SELECT query, calls, total_time, mean_time 
FROM pg_stat_statements 
ORDER BY total_time DESC 
LIMIT 10;
```

---

**Version**: 1.0.0  
**Last Updated**: June 2026  
**Status**: Production Ready ✅

For additional support, review logs and application health endpoint.
