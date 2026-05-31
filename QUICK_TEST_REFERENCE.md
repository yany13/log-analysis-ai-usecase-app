# ByteBite Failure Injection - Quick Test Reference

## Setup (One-time)

```bash
# 1. Enable failure injection in docker-compose.yml
#    Change: FAILURE_INJECTION_ENABLED: "false"
#    To:     FAILURE_INJECTION_ENABLED: "true"

# 2. Rebuild and restart
mvn clean package -DskipTests
docker-compose down
docker-compose up -d

# 3. Wait for services to be healthy
docker-compose ps  # All should show "healthy" or "up"

# 4. Monitor logs in separate terminal
docker-compose logs -f tomcat
```

---

## Test 1: DB Connection Leak (/menu)

**Single request (should work):**
```bash
curl http://localhost:8080/bytebite/menu/api | jq
```

**Expected response:**
```json
[
  {"id": 1, "name": "Pizza", "price": 12.99, "available": true},
  {"id": 2, "name": "Burger", "price": 10.99, "available": true},
  ...
]
```

**Exhaust pool (20-30 requests):**
```bash
for i in {1..30}; do 
  echo "Request $i"
  curl -s http://localhost:8080/bytebite/menu/api > /dev/null
  sleep 0.5
done
```

**Expected error around request 21+:**
```
java.sql.SQLException: Connection is not available, request timed out after 30000ms
```

**Watch logs for:**
```
⚠️ Connection leak scenario: Got connection [xxxxx] from pool but NOT closing it
❌ CONNECTION LEAK: Statement closed but Connection kept open
```

---

## Test 2: LDAP Timeout (/login)

**Attempt login with failure injection:**
```bash
curl -X POST http://localhost:8080/bytebite/login \
  -d "username=admin&password=admin_secure_pwd_123" \
  -L  # Follow redirect to menu if auth succeeds
```

**Expected failure (1ms timeout):**
```
javax.naming.CommunicationException: connection timed out
```

**Watch logs for:**
```
⚠️ LDAP Failure Injection: Using invalid connection timeout
LDAP authentication failed for user: admin
❌ LDAP FAILURE SCENARIO: CommunicationException
```

**What's happening:**
- Code tries to connect to LDAP with 1ms timeout
- LDAP server can't respond in time
- Connection fails immediately

---

## Test 3: OutOfMemoryError (/analytics)

**Generate some analytics first (optional):**
```bash
for i in {1..100}; do
  curl -X POST http://localhost:8080/bytebite/api/analytics \
    -H "Content-Type: application/json" \
    -d '{"eventType":"PAGE_VIEW","page":"menu"}'
done
```

**Trigger all-time report (causes OOM):**
```bash
curl "http://localhost:8080/bytebite/analytics/api?action=all-time-report" \
  --max-time 30
```

**Expected (after 10-30 seconds):**
```
java.lang.OutOfMemoryError: Java heap space
```

**Watch logs for:**
```
⚠️ OOM FAILURE INJECTION: Loading all-time analytics without pagination
Loaded 1000 events into memory (heap usage increasing)
Loaded 2000 events into memory (heap usage increasing)
...
❌ OOM FAILURE SCENARIO TRIGGERED: OutOfMemoryError
```

**Note:** Tomcat may need restart after OOM

---

## Test 4: Thread Deadlock (/kds)

**Submit order update with deadlock:**
```bash
curl -X POST http://localhost:8080/bytebite/kds \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-TEST001","status":"IN_PROGRESS"}' \
  -G --data-urlencode "injectDeadlock=true"
```

**Expected response:**
```json
{"status": "updated"}
```

**But in logs:**
```
⚠️ DEADLOCK FAILURE INJECTION: Creating deadlock scenario
Thread-OrderUpdateWorker-1: Acquired orderLock
Thread-InventoryUpdateWorker-2: Acquired inventoryLock
❌ DEADLOCK SCENARIO: Both threads are now BLOCKED
Thread-1 state: WAITING (waiting for inventoryLock)
Thread-2 state: WAITING (waiting for orderLock)
```

**Capture thread dump:**
```bash
# Method 1: jstack in container
docker exec bytebite-tomcat bash -c 'jstack 1' | tee threaddump.txt

# Method 2: If jstack available locally
# Get Tomcat PID from docker stats
PID=$(docker inspect -f '{{.State.Pid}}' bytebite-tomcat)
jstack $PID > threaddump.txt
```

**Look for in jstack:**
```
Found one Java-level deadlock:
=============================
"OrderUpdateWorker-1":
  waiting for ownable synchronizer 0x00007f... (a java.lang.Object)
  which is held by "InventoryUpdateWorker-2"
"InventoryUpdateWorker-2":
  waiting for ownable synchronizer 0x00007f... (a java.lang.Object)
  which is held by "OrderUpdateWorker-1"
```

---

## Test 5: SSL Certificate Failure (/checkout)

**Submit order with SSL failure:**
```bash
curl -X POST http://localhost:8080/bytebite/checkout/api \
  -H "Content-Type: application/json" \
  -d '{"totalAmount":99.99}' \
  -G --data-urlencode "injectSSLFailure=true" \
  -w "\nHTTP Status: %{http_code}\n"
```

**Expected response:**
```json
{
  "status": "error",
  "message": "SSL Certificate Validation Failed: ..."
}
```

**HTTP Status:** 502

**Watch logs for:**
```
⚠️ SSL FAILURE INJECTION: Attempting connection to self-signed HTTPS endpoint
Attempting HTTPS connection to: https://self-signed.example.com:8443/payment-gateway
javax.net.ssl.SSLHandshakeException: PKIX path building failed: 
  sun.security.provider.certpath.SunCertPathBuilderException: 
  unable to find valid certification path to requested target
❌ SSL HANDSHAKE FAILURE SCENARIO: PKIX path building failed
```

---

## Health Check

**Check overall health:**
```bash
curl http://localhost:8080/bytebite/health | jq .
```

**Normal output:**
```json
{
  "status": "UP",
  "timestamp": 1748734800000,
  "service": "ByteBite",
  "version": "1.0.0",
  "database": {
    "status": "UP",
    "message": "Database connection successful"
  },
  "ldap": {
    "status": "UP",
    "message": "LDAP configured at openldap:389"
  }
}
```

**During DB connection leak:**
```json
{
  "status": "DEGRADED",
  "database": {
    "status": "DOWN",
    "message": "Connection is not available, request timed out..."
  },
  "ldap": {"status": "UP"}
}
```

---

## Log Analysis Commands

**Extract all failure markers:**
```bash
docker-compose logs tomcat | grep -E "⚠️|❌"
```

**Find specific exception:**
```bash
docker-compose logs tomcat | grep -E "SQLException|CommunicationException|OutOfMemoryError|Deadlock|SSLHandshakeException"
```

**Get full failure scenario logs:**
```bash
docker-compose logs tomcat | grep -B2 -A5 "FAILURE INJECTION ACTIVATED"
```

**Extract for documentation:**
```bash
docker logs bytebite-tomcat > failure_test_logs.txt
cat failure_test_logs.txt | grep -E "Request|FAILURE|SCENARIO|Exception" > summary.txt
```

---

## Troubleshooting

### "Connection is not available" not appearing:
```bash
# Increase number of requests to exhaust pool faster
for i in {1..50}; do curl -s http://localhost:8080/bytebite/menu/api; done
```

### LDAP test succeeding when should fail:
```bash
# Check LDAP is running
docker-compose ps | grep ldap
# Check timeout is actually 1ms in logs
docker-compose logs tomcat | grep timeout
```

### OOM not triggering:
```bash
# Verify heap size (may need more data)
docker-compose logs tomcat | grep -i "memory\|heap"
# Manually generate more analytics events
for i in {1..1000}; do curl -s -X POST http://localhost:8080/bytebite/api/analytics \
  -H "Content-Type: application/json" -d '{"eventType":"TEST"}'; done
```

### Deadlock threads not visible:
```bash
# Verify thread names in logs
docker-compose logs tomcat | grep -i "thread\|worker"
# Get more detailed jstack
docker exec bytebite-tomcat bash -c 'jstack 1 | grep -A5 "OrderUpdate\|Inventory"'
```

### SSL test not working:
```bash
# Verify endpoint parameter is passed correctly
curl -v -X POST http://localhost:8080/bytebite/checkout/api \
  -G --data-urlencode "injectSSLFailure=true" 2>&1 | grep -i ssl
```

---

## Reset / Cleanup

**Restart Tomcat after OOM/deadlock:**
```bash
docker-compose restart tomcat
docker-compose logs -f tomcat  # Wait for "starting" messages
```

**Reset all containers:**
```bash
docker-compose down -v
docker-compose up -d
```

**Clear all logs:**
```bash
docker exec bytebite-tomcat rm /usr/local/tomcat/logs/catalina.out
docker-compose restart tomcat
```

---

## Test Checklist

**Before running tests:**
- [ ] `FAILURE_INJECTION_ENABLED: "true"` in docker-compose.yml
- [ ] `mvn clean package` completed
- [ ] All containers healthy: `docker-compose ps`
- [ ] Terminal ready to monitor: `docker-compose logs -f tomcat`

**For each test:**
- [ ] Curl command executed
- [ ] Expected log markers appear
- [ ] Exception type matches documentation
- [ ] Health endpoint shows appropriate status
- [ ] Tomcat recovers or needs restart

**Documentation gathering:**
- [ ] Logs captured: `docker logs bytebite-tomcat > logs.txt`
- [ ] Thread dumps captured (if deadlock): `docker exec bytebite-tomcat jstack 1 > threads.txt`
- [ ] Screenshots of curl responses
- [ ] JSON responses validated

---

## Fast Test Sequence (5 minutes)

```bash
# 1. DB Leak (2 min)
for i in {1..25}; do curl -s http://localhost:8080/bytebite/menu/api; done

# 2. LDAP Timeout (1 min)
curl -X POST http://localhost:8080/bytebite/login \
  -d "username=admin&password=admin_secure_pwd_123"

# 3. Deadlock (1 min)
curl -X POST http://localhost:8080/bytebite/kds \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD1","status":"IN_PROGRESS"}' \
  -G --data-urlencode "injectDeadlock=true"

# 4. SSL Failure (1 min)
curl -X POST http://localhost:8080/bytebite/checkout/api \
  -H "Content-Type: application/json" \
  -d '{"totalAmount":99}' \
  -G --data-urlencode "injectSSLFailure=true"

# Review logs
docker-compose logs tomcat | grep "⚠️\|❌" | tail -20
```

---

## Production Comparison Notes

These failure scenarios simulate real production issues:

| Scenario | Real Cause | Common in Production |
|----------|-----------|---------------------|
| DB Leak | Resource not closed in exception path | YES - Very common |
| LDAP Timeout | Network issues, LDAP server slow | YES - Identity system problems |
| OOM | Unbounded caching, memory leak | YES - Long-running apps |
| Deadlock | Inverted lock ordering | YES - Complex data updates |
| SSL | Cert expired, truststore missing | YES - Integration failures |

All scenarios demonstrate genuine Java problems that engineers encounter.
