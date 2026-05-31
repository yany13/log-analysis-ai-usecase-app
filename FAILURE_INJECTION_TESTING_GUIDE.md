# ByteBite Failure Injection Testing Guide

## Overview

This guide provides step-by-step instructions to test each of the 5 intentional failure injection scenarios implemented in ByteBite. Each scenario simulates a common Java/Tomcat production issue and generates diagnostic evidence suitable for an engineering book on Java debugging.

---

## Prerequisites

1. **Docker environment running** with all services healthy:
   ```bash
   docker-compose up -d
   docker-compose ps  # Verify all services are healthy
   ```

2. **Access to Tomcat logs**:
   ```bash
   # Live tail of logs
   docker-compose logs -f tomcat
   
   # Or directly read
   docker exec bytebite-tomcat tail -f /usr/local/tomcat/logs/catalina.out
   ```

3. **Enable failure injection** in docker-compose.yml:
   ```yaml
   environment:
     FAILURE_INJECTION_ENABLED: "true"
   ```

4. **Rebuild the WAR file**:
   ```bash
   mvn clean package -DskipTests
   ```

5. **Restart Tomcat**:
   ```bash
   docker-compose restart tomcat
   ```

---

## Test 1: DB Connection Leak (MenuServlet - /menu)

### Objective
Exhaust the database connection pool by repeatedly requesting menu items without closing connections.

### How It Works
- The `MenuServlet.buildMenu()` method intentionally skips closing the database connection
- Each request gets a connection but doesn't return it to the pool
- After ~20 requests (pool exhaustion threshold), subsequent requests fail with timeout

### Testing Steps

1. **Enable failure injection and rebuild**:
   ```bash
   # Already enabled in docker-compose.yml
   docker-compose restart tomcat
   ```

2. **Verify initial menu works**:
   ```bash
   curl http://localhost:8080/bytebite/menu/api
   ```

3. **Generate repeated requests to exhaust pool** (open another terminal):
   ```bash
   #!/bin/bash
   for i in {1..30}; do
     echo "Request $i..."
     curl http://localhost:8080/bytebite/menu/api
     sleep 0.5
   done
   ```

4. **Monitor Tomcat logs** for:
   ```
   ⚠️ FAILURE INJECTION ACTIVATED: db_leak
   ⚠️ Connection leak scenario: Got connection [hash] from pool but NOT closing it
   ❌ CONNECTION LEAK: Statement closed but Connection kept open
   java.sql.SQLException: Connection is not available, request timed out after Xms
   HikariPool-N - Connection is not available, request timed out
   ```

5. **Expected behavior**:
   - First ~20 requests succeed
   - Requests 21+ fail with SQLException
   - Pool metrics show maxPoolSize connections all in use
   - Subsequent requests block, waiting for a connection

### Diagnostic Evidence Location
- **Log file**: `catalina.out` lines with "CONNECTION LEAK"
- **Pool exhaustion**: "Connection is not available" error message
- **Recovery**: Restart Tomcat to reset pool

---

## Test 2: LDAP Authentication Timeout (LoginServlet - /login)

### Objective
Trigger LDAP connection timeout by using invalid LDAP configuration.

### How It Works
- `LoginServlet.authenticateUser()` attempts LDAP bind with 1ms timeout (instead of 5000ms)
- LDAP server cannot respond in 1ms
- Results in `javax.naming.CommunicationException: connection timed out`

### Testing Steps

1. **Verify LDAP is running**:
   ```bash
   docker-compose ps | grep openldap
   # Should show: bytebite-openldap ... healthy
   ```

2. **Attempt login with failure injection**:
   ```bash
   curl -X POST http://localhost:8080/bytebite/login \
     -d "username=admin&password=admin_secure_pwd_123"
   ```

3. **Expected failure** (due to 1ms timeout):
   - Should fail with timeout
   - Log shows connection timeout exception

4. **Check Tomcat logs** for:
   ```
   ⚠️ FAILURE INJECTION ACTIVATED: ldap_timeout
   ⚠️ LDAP Failure Injection: Using invalid connection timeout
   Attempting LDAP authentication for user: admin
   javax.naming.CommunicationException: connection timed out
   LDAP authentication failed for user: admin
   ❌ LDAP FAILURE SCENARIO: CommunicationException
   ```

5. **Real LDAP commands** (optional, to verify LDAP is running):
   ```bash
   # Test LDAP connectivity from host
   ldapsearch -x -H ldap://localhost:389 \
     -b dc=bytebite,dc=local \
     -D "cn=admin,dc=bytebite,dc=local" \
     -w "admin_secure_pwd_123" \
     "(uid=*)"
   ```

### Diagnostic Evidence Location
- **Exception type**: `javax.naming.CommunicationException`
- **Root cause**: Connection timeout after 1ms
- **Error message**: Shows LDAP host and port configuration
- **Log context**: Shows LDAP URL being used

---

## Test 3: OutOfMemoryError (AnalyticsServlet - /analytics)

### Objective
Exhaust Java heap memory by loading entire analytics dataset into memory without pagination.

### How It Works
- Query endpoint: `GET /bytebite/analytics/api?action=all-time-report`
- Loads ALL analytics_events records into unbounded `List<Map>`
- Adds 100KB fake payload to each event to accelerate OOM
- JVM heap fills until `OutOfMemoryError: Java heap space`

### Testing Steps

1. **Optional: Reduce heap size for faster OOM** (edit docker-compose.yml):
   ```yaml
   environment:
     JAVA_OPTS: "-Xmx512m -Xms256m"  # Instead of default 1GB+
   ```

2. **Populate analytics database** (generate some events):
   ```bash
   # Make a few analytics POST calls
   for i in {1..100}; do
     curl -X POST http://localhost:8080/bytebite/api/analytics \
       -H "Content-Type: application/json" \
       -d '{"eventType":"PAGE_VIEW","page":"menu"}'
   done
   ```

3. **Trigger all-time report** (causes OOM):
   ```bash
   curl "http://localhost:8080/bytebite/analytics/api?action=all-time-report"
   ```

4. **Monitor Tomcat logs** for OOM progression:
   ```
   ⚠️ OOM FAILURE INJECTION: Loading all-time analytics without pagination
   Generating all-time analytics report - loading ALL data into memory
   Loaded 1000 events into memory (heap usage increasing)
   Loaded 2000 events into memory (heap usage increasing)
   ...
   java.lang.OutOfMemoryError: Java heap space
   ❌ OOM FAILURE SCENARIO TRIGGERED: OutOfMemoryError
   ```

5. **Expected behavior**:
   - Request hangs while loading data
   - Tomcat logs show increasing event counts
   - Eventually: `OutOfMemoryError: Java heap space`
   - Tomcat worker thread dies
   - JVM may trigger full GC and take time to recover

### Diagnostic Evidence Location
- **Exception**: `java.lang.OutOfMemoryError: Java heap space`
- **Stack trace**: Shows ArrayList.grow() and memory allocation methods
- **Log context**: Shows query being executed and event counts
- **GC logs**: May show GC overhead attempts before failure

### Note on Testing OOM
- OOM is extremely hard to recover from
- Tomcat may become unresponsive after OOM
- Best practice: Run OOM test with a dedicated Tomcat instance or smaller heap
- Document the exact sequence of events for the book

---

## Test 4: Thread Deadlock (KDSServlet - /kds)

### Objective
Create a classic deadlock scenario with two threads competing for locks in opposite order.

### How It Works
- `updateOrderStatusWithDeadlock()` creates two worker threads
- Thread 1: Locks `orderLock` then tries to acquire `inventoryLock`
- Thread 2: Locks `inventoryLock` then tries to acquire `orderLock`
- Result: Both threads BLOCKED indefinitely

### Testing Steps

1. **Submit order update with deadlock injection**:
   ```bash
   curl -X POST http://localhost:8080/bytebite/kds \
     -H "Content-Type: application/json" \
     -d '{"orderId":"ORD-12345","status":"IN_PROGRESS"}' \
     -G --data-urlencode "injectDeadlock=true"
   ```

2. **Check Tomcat logs** for deadlock detection:
   ```
   ⚠️ DEADLOCK FAILURE INJECTION: Creating deadlock scenario
   ⚠️ Creating deadlock with order ORD-12345 and status IN_PROGRESS
   
   Thread-1: Attempting to acquire orderLock
   Thread-1: Acquired orderLock
   Thread-2: Attempting to acquire inventoryLock
   Thread-2: Acquired inventoryLock
   
   Thread-1: Attempting to acquire inventoryLock
   Thread-2: Attempting to acquire orderLock
   
   ❌ DEADLOCK SCENARIO: Both threads are now BLOCKED
   Thread-1 state: WAITING (waiting for inventoryLock)
   Thread-2 state: WAITING (waiting for orderLock)
   ```

3. **Capture thread dump** to show deadlock:
   ```bash
   # Inside Tomcat container
   jstack $(pgrep -f tomcat) | grep -A 20 "deadlock\|OrderUpdateWorker\|InventoryUpdateWorker"
   
   # Or get container PID and run locally
   docker exec bytebite-tomcat jstack 1 > threaddump.txt
   ```

4. **Expected jstack output** (shows circular wait):
   ```
   "OrderUpdateWorker-1":
     - waiting to lock <0x00007f8c9d0c8d80> (a java.lang.Object)
     - locked <0x00007f8c9d0c8d50> (a java.lang.Object)
   
   "InventoryUpdateWorker-2":
     - waiting to lock <0x00007f8c9d0c8d50> (a java.lang.Object)
     - locked <0x00007f8c9d0c8d80> (a java.lang.Object)
   
   Found one Java-level deadlock:
   =============================
   "OrderUpdateWorker-1":
     waiting for ownable synchronizer 0x00007f8c9d0c8d80, (a java.util.concurrent.locks.ReentrantLock$NonfairSync)
     which is held by "InventoryUpdateWorker-2"
   "InventoryUpdateWorker-2":
     waiting for ownable synchronizer 0x00007f8c9d0c8d50, (a java.util.concurrent.locks.ReentrantLock$NonfairSync)
     which is held by "OrderUpdateWorker-1"
   ```

5. **Verify health endpoint** shows degradation:
   ```bash
   curl http://localhost:8080/bytebite/health
   # Should show overall status DEGRADED if health check uses KDS endpoint
   ```

### Diagnostic Evidence Location
- **Thread dump**: Shows WAITING states and lock ownership
- **Logs**: Show thread names and state transitions
- **Lock objects**: Identified by memory address in jstack output
- **Circular dependency**: Evident from waiting-locked relationships

---

## Test 5: SSL Certificate Handshake Failure (CheckoutServlet - /checkout)

### Objective
Trigger SSL handshake failure by attempting HTTPS connection to self-signed certificate endpoint.

### How It Works
- `CheckoutServlet.testSSLHandshakeFailure()` attempts HTTPS to non-existent endpoint
- JDK truststore doesn't contain the self-signed certificate
- PKIX path validation fails
- Results in `javax.net.ssl.SSLHandshakeException`

### Testing Steps

1. **Submit order with SSL failure injection**:
   ```bash
   curl -X POST http://localhost:8080/bytebite/checkout/api \
     -H "Content-Type: application/json" \
     -d '{"totalAmount":49.99}' \
     -G --data-urlencode "injectSSLFailure=true"
   ```

2. **Monitor Tomcat logs** for SSL handshake failure:
   ```
   ⚠️ SSL FAILURE INJECTION: Attempting connection to self-signed HTTPS endpoint
   Attempting HTTPS connection to: https://self-signed.example.com:8443/payment-gateway
   Initiating SSL handshake with self-signed.example.com
   
   javax.net.ssl.SSLHandshakeException: PKIX path building failed: 
     sun.security.provider.certpath.SunCertPathBuilderException: 
     unable to find valid certification path to requested target
   
   ❌ SSL HANDSHAKE FAILURE SCENARIO: PKIX path building failed
   ```

3. **Response will contain error**:
   ```json
   {
     "status": "error",
     "message": "SSL Certificate Validation Failed: ..."
   }
   ```

4. **HTTP status**: 502 Bad Gateway (indicating downstream SSL issue)

5. **Enhanced testing** (if you want to use actual self-signed cert):
   - Create test endpoint with self-signed cert
   - Update URL in `testSSLHandshakeFailure()` to point to it
   - Certificate validation will fail as expected

### Diagnostic Evidence Location
- **Exception type**: `javax.net.ssl.SSLHandshakeException`
- **Root cause**: `sun.security.provider.certpath.SunCertPathBuilderException`
- **Error message**: "unable to find valid certification path to requested target"
- **Truststore**: JDK default cacerts or custom keystore
- **Certificate chain**: Shows where validation failed

---

## Health Endpoint Behavior Under Failure

The `/health` endpoint should degrade gracefully:

```bash
# Before failure injection
curl http://localhost:8080/bytebite/health | jq .
{
  "status": "UP",
  "database": {"status": "UP"},
  "ldap": {"status": "UP"}
}

# After connection leak exhausts pool
curl http://localhost:8080/bytebite/health | jq .
{
  "status": "DEGRADED",
  "database": {"status": "DOWN", "message": "Connection is not available..."},
  "ldap": {"status": "UP"}
}
```

---

## Log Analysis Workflow

### 1. Capture all relevant logs:
```bash
docker logs bytebite-tomcat > tomcat_logs.txt
docker exec bytebite-tomcat cat /usr/local/tomcat/logs/catalina.out > catalina_out.txt
```

### 2. Search for failure markers:
```bash
grep -i "FAILURE INJECTION\|connection leak\|communication exception\|OutOfMemoryError\|DEADLOCK\|SSL.*FAILURE" *.txt
```

### 3. Extract stack traces:
```bash
# For specific exception
grep -A 30 "SSLHandshakeException" catalina_out.txt
```

### 4. Timeline reconstruction:
```bash
# Show timestamps of key events
grep -E "^\d{4}-\d{2}-\d{2}|⚠️|❌" catalina_out.txt
```

---

## Troubleshooting

### Test not triggering failure:
1. Verify `FAILURE_INJECTION_ENABLED: "true"` in docker-compose.yml
2. Rebuild WAR: `mvn clean package -DskipTests`
3. Check Tomcat logs for "FAILURE INJECTION ACTIVATED" message

### Logs not appearing:
1. Verify logging level: `LOG_LEVEL: INFO` (or DEBUG)
2. Check that Tomcat is running: `docker-compose ps`
3. Review Tomcat startup logs: `docker logs bytebite-tomcat`

### Connection pool not exhausting:
1. Decrease pool size in config to test faster
2. Increase request rate
3. Verify connection is actually leaked (check connection count)

### LDAP test timing out instead of showing auth error:
1. Verify OpenLDAP is running: `docker-compose ps | grep ldap`
2. Check LDAP logs: `docker logs bytebite-openldap`
3. Try manual LDAP search to verify service is responding

### OOM test not triggering:
1. Check heap size: OOM needs significant memory pressure
2. Increase analytics data in database
3. Monitor GC before OOM occurs

### Deadlock thread names not visible in logs:
1. Adjust thread naming in deadlock method
2. Capture jstack output for proper thread visualization
3. Run jstack on container PID separately

---

## Summary Table

| Scenario | Endpoint | Parameter | Expected Exception | Log Marker |
|----------|----------|-----------|-------------------|-----------|
| DB Leak | `/menu/api` | (automatic) | SQLException: Connection not available | CONNECTION LEAK |
| LDAP Timeout | `/login` POST | (automatic) | CommunicationException: connection timed out | LDAP FAILURE |
| OOM | `/analytics/api?action=all-time-report` | (automatic) | OutOfMemoryError: Java heap space | OOM FAILURE |
| Deadlock | `/kds` POST | `injectDeadlock=true` | (no exception, BLOCKED state) | DEADLOCK SCENARIO |
| SSL | `/checkout/api` POST | `injectSSLFailure=true` | SSLHandshakeException | SSL FAILURE |

---

## Documentation for Engineering Book

Each scenario produces clean, analytically clear diagnostic evidence suitable for a Java debugging book:

1. **Use cases** show real production problems
2. **Error messages** match actual Java exceptions  
3. **Log sequences** demonstrate problem progression
4. **Diagnostic tools** (jstack, logs, health checks) show investigation path
5. **Recovery steps** explain how to fix or mitigate each issue

Record complete logs, thread dumps, and heap dumps for book publication.
