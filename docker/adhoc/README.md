# ByteBite Adhoc Container

The adhoc container handles all initialization and automation tasks for the ByteBite application.

## Purpose

This container:
- ✅ Waits for all dependencies (OpenLDAP, PostgreSQL) to be healthy
- ✅ Initializes LDAP with default users
- ✅ Initializes PostgreSQL database schema
- ✅ Validates all services are ready
- ✅ Exits after completion (runs once per startup)

## Flow

```
docker-compose up
    ↓
OpenLDAP, PostgreSQL start
    ↓
Adhoc container waits for healthy status
    ↓
Adhoc initializes LDAP (creates ou=people, adds users)
    ↓
Adhoc initializes Database
    ↓
Adhoc exits (restart: no)
    ↓
Tomcat starts (depends on adhoc)
    ↓
Tomcat can authenticate against LDAP immediately
```

## Files

### Scripts (in `/scripts`)
Each script is independent and idempotent (safe to run multiple times):

- **init.sh** - Main orchestrator (calls all other scripts)
- **wait-for-ldap.sh** - Waits for OpenLDAP to be healthy
- **wait-for-postgres.sh** - Waits for PostgreSQL to be ready
- **init-ldap.sh** - Loads LDIF file to initialize LDAP data
- **init-db.sh** - Initializes database schema if needed

### LDIF Bootstrap File
- **bootstrap.ldif** - Contains all LDAP organizational units, users, and groups
  - Organizational Units: ou=people, ou=groups
  - Users: admin, manager, staff1, staff2, kds_operator, customer1
  - Groups: administrators, managers, staff, kds, customers
  - All users have password: `admin`

## How to Extend

Add new initialization steps:

1. **Create a new script** in `/scripts`:
   ```bash
   scripts/setup-elasticsearch.sh
   ```

2. **Add to init.sh**:
   ```bash
   echo "🔍 Initializing Elasticsearch..."
   ./scripts/setup-elasticsearch.sh
   ```

3. **Rebuild container**:
   ```bash
   docker-compose build --no-cache adhoc
   docker-compose up -d
   ```

## Running Standalone

You can run just the adhoc container:

```bash
# Re-run initialization
docker-compose up adhoc

# View logs
docker-compose logs adhoc

# Rebuild and run
docker-compose build --no-cache adhoc && docker-compose up adhoc
```

## Environment Variables

All scripts use environment variables from docker-compose.yml:

```yaml
environment:
  LDAP_HOST: openldap
  LDAP_PORT: 389
  LDAP_BASE_DN: dc=bytebite,dc=local
  DB_HOST: postgres
  DB_PORT: 5432
  DB_NAME: bytebite_db
```

Modify these in docker-compose.yml to customize behavior.

## Logs

View initialization output:

```bash
docker-compose logs adhoc
```

Check for specific issues:
```bash
docker-compose logs adhoc | grep -E "✅|❌|⚠️"
```

## Troubleshooting

If adhoc fails:

1. Check logs:
   ```bash
   docker-compose logs adhoc
   ```

2. Verify dependencies are ready:
   ```bash
   docker-compose ps
   # All should show "healthy"
   ```

3. Re-run initialization:
   ```bash
   docker-compose down && docker-compose up -d
   ```

4. Run individual script:
   ```bash
   docker-compose exec adhoc bash
   ./scripts/init-ldap.sh
   ```
