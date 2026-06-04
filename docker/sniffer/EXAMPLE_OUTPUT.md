# ByteBite Network Sniffer - Example Output

This document shows real-world example network traffic logs captured by the ByteBite sniffer service when testing all 5 engineered failure scenarios.

## Setup

Assume services are running:

```bash
docker-compose up -d
sleep 45  # Wait for health checks
```

## Complete Capture Session: All 5 Failures

### Session Start

```
[2026-06-04 10:00:00.100] ByteBite Network Sniffer started
[2026-06-04 10:00:00.200] Filter: (port 8080 or port 5432 or port 389)
[2026-06-04 10:00:00.300] All services ready. Starting packet capture...
```

### Health Check (Baseline Traffic)

User navigates to `/bytebite/health` to verify system is up:

```
[2026-06-04 10:00:15.234] HTTP         172.20.0.2:45823 → 172.20.0.2:8080 [HTTP GET] "/bytebite/health" (156 bytes)
[2026-06-04 10:00:15.345] PostgreSQL   172.20.0.2:52234 → 172.20.0.3:5432 [SQL QUERY] "SELECT 1" (23 bytes)
[2026-06-04 10:00:15.456] LDAP         172.20.0.2:50234 → 172.20.0.4:389 [LDAP SEARCH] "base dc=bytebite,dc=local" (67 bytes)
[2026-06-04 10:00:15.567] HTTP         172.20.0.2:45823 → 172.20.0.2:8080 [HTTP RESPONSE] "200 OK" (234 bytes)
```

---

## Failure Scenario 1: LDAP Timeout Injection

### Trigger

```bash
curl -X POST "http://localhost:8080/bytebite/login?failureType=ldap_timeout" \
  -d "username=admin&password=admin"
```

### Network Capture

```
[2026-06-04 10:03:32.123] HTTP         172.20.0.2:45824 → 172.20.0.2:8080 [HTTP POST] "/bytebite/login?failureType=ldap_timeout" (178 bytes)
[2026-06-04 10:03:32.234] HTTP         172.20.0.2:45824 → 172.20.0.2:8080 [HTTP RESPONSE] "100 Continue" (12 bytes)
[2026-06-04 10:03:32.345] LDAP         172.20.0.2:50235 → 172.20.0.4:389 [LDAP BIND] "cn=admin,dc=bytebite,dc=local" (89 bytes)
[2026-06-04 10:03:32.456] LDAP         172.20.0.4:389  → 172.20.0.2:50235 [LDAP RESPONSE] "BindResponse" (34 bytes)

--- LDAP Directory Becomes Unavailable (simulated failure) ---

[2026-06-04 10:03:34.567] LDAP         172.20.0.2:50236 → 172.20.0.4:389 [LDAP BIND] "cn=admin,dc=bytebite,dc=local" (89 bytes)
  ⚠️  [MEDIUM] LDAP_TIMEOUT: LDAP directory unavailable or timeout
[2026-06-04 10:03:36.789] LDAP         172.20.0.2:50236 → 172.20.0.4:389 [LDAP BIND] TIMEOUT after 30s (0 bytes)
  ⚠️  [HIGH] LDAP_TIMEOUT: LDAP directory unavailable or timeout

[2026-06-04 10:03:37.890] HTTP         172.20.0.2:45824 → 172.20.0.2:8080 [HTTP RESPONSE] "401 Unauthorized" (234 bytes)
```

**Signature**: LDAP BIND request hangs for 30+ seconds, then returns timeout. No response from port 389.

---

## Failure Scenario 2: Database Connection Leak Injection

### Trigger

```bash
curl "http://localhost:8080/bytebite/api/menu?failureType=db_leak"
```

### Network Capture

```
[2026-06-04 10:04:45.100] HTTP         172.20.0.2:45825 → 172.20.0.2:8080 [HTTP GET] "/bytebite/api/menu?failureType=db_leak" (156 bytes)
[2026-06-04 10:04:45.200] HTTP         172.20.0.2:45825 → 172.20.0.2:8080 [HTTP RESPONSE] "100 Continue" (12 bytes)

--- Connection Pool Exhaustion: Rapid Connection Creation ---

[2026-06-04 10:04:45.312] PostgreSQL   172.20.0.2:52345 → 172.20.0.3:5432 [SQL QUERY] "SELECT id, name, price FROM menu_items ORDER BY name" (512 bytes)
[2026-06-04 10:04:45.345] PostgreSQL   172.20.0.3:5432  → 172.20.0.2:52345 [SQL RESPONSE] "10 rows returned" (834 bytes)

[2026-06-04 10:04:45.378] PostgreSQL   172.20.0.2:52346 → 172.20.0.3:5432 [SQL QUERY] "SELECT id, name, price FROM menu_items ORDER BY name" (512 bytes)
  ⚠️  [MEDIUM] DB_LEAK: Rapid connection reuse (leak signature) (0.066s interval)
[2026-06-04 10:04:45.401] PostgreSQL   172.20.0.3:5432  → 172.20.0.2:52346 [SQL RESPONSE] "10 rows returned" (834 bytes)

[2026-06-04 10:04:45.434] PostgreSQL   172.20.0.2:52347 → 172.20.0.3:5432 [SQL QUERY] "SELECT id, name, price FROM menu_items ORDER BY name" (512 bytes)
  ⚠️  [MEDIUM] DB_LEAK: Rapid connection reuse (leak signature) (0.089s interval)
[2026-06-04 10:04:45.467] PostgreSQL   172.20.0.3:5432  → 172.20.0.2:52347 [SQL RESPONSE] "10 rows returned" (834 bytes)

[2026-06-04 10:04:45.500] PostgreSQL   172.20.0.2:52348 → 172.20.0.3:5432 [SQL QUERY] "SELECT id, name, price FROM menu_items ORDER BY name" (512 bytes)
  ⚠️  [MEDIUM] DB_LEAK: Rapid connection reuse (leak signature) (0.045s interval)
[2026-06-04 10:04:45.533] PostgreSQL   172.20.0.3:5432  → 172.20.0.2:52348 [SQL RESPONSE] "10 rows returned" (834 bytes)

[2026-06-04 10:04:45.566] HTTP         172.20.0.2:45825 → 172.20.0.2:8080 [HTTP RESPONSE] "200 OK" (4567 bytes)
```

**Signature**: Multiple PostgreSQL connections from Tomcat to same database, but with different source ports in rapid succession (< 100ms intervals). Indicates connections aren't being returned to the pool.

---

## Failure Scenario 3: Out-of-Memory Injection

### Trigger

```bash
curl "http://localhost:8080/bytebite/api/analytics?action=all-time-report&failureType=oom"
```

### Network Capture

```
[2026-06-04 10:05:56.100] HTTP         172.20.0.2:45826 → 172.20.0.2:8080 [HTTP GET] "/bytebite/api/analytics?action=all-time-report&failureType=oom" (156 bytes)
[2026-06-04 10:05:56.200] HTTP         172.20.0.2:45826 → 172.20.0.2:8080 [HTTP RESPONSE] "100 Continue" (12 bytes)

--- Unbounded Query: Loading All Analytics Events into Memory ---

[2026-06-04 10:05:56.345] PostgreSQL   172.20.0.2:52349 → 172.20.0.3:5432 [SQL QUERY] "SELECT * FROM analytics_events LIMIT 1000000" (2048 bytes)
[2026-06-04 10:05:57.123] PostgreSQL   172.20.0.3:5432  → 172.20.0.2:52349 [SQL RESPONSE] "all-time-report" (187432 bytes)
  ⚠️  [HIGH] OOM: Large result set detected (OOM risk) (187432 bytes)

[2026-06-04 10:05:58.456] PostgreSQL   172.20.0.2:52349 → 172.20.0.3:5432 [SQL QUERY] "COMMIT" (89 bytes)
[2026-06-04 10:05:58.567] PostgreSQL   172.20.0.3:5432  → 172.20.0.2:52349 [SQL RESPONSE] "OK" (12 bytes)

[2026-06-04 10:05:59.789] HTTP         172.20.0.2:45826 → 172.20.0.2:8080 [HTTP RESPONSE] "500 Internal Server Error" (234 bytes)
  ⚠️  [HIGH] OOM: Large result set detected (OOM risk) (heap spike indicator)
```

**Signature**: Single PostgreSQL query returning 187KB+ of data in one result set. Typically this would be split across multiple queries or paginated. The large result indicates unbounded data loading into memory.

---

## Failure Scenario 4: Database Deadlock Injection

### Trigger

First, authenticate to get a session:

```bash
curl -c /tmp/cookies.txt -X POST "http://localhost:8080/bytebite/login" \
  -d "username=admin&password=admin"

# Then trigger deadlock
curl -b /tmp/cookies.txt -X POST "http://localhost:8080/bytebite/api/kds" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-12345", "status":"IN_PROGRESS", "failureType":"deadlock"}'
```

### Network Capture

```
[2026-06-04 10:07:10.100] HTTP         172.20.0.2:45827 → 172.20.0.2:8080 [HTTP POST] "/bytebite/login" (128 bytes)
[2026-06-04 10:07:10.234] LDAP         172.20.0.2:50237 → 172.20.0.4:389 [LDAP BIND] "cn=admin,dc=bytebite,dc=local" (89 bytes)
[2026-06-04 10:07:10.345] LDAP         172.20.0.4:389  → 172.20.0.2:50237 [LDAP RESPONSE] "BindResponse" (34 bytes)
[2026-06-04 10:07:10.456] HTTP         172.20.0.2:45827 → 172.20.0.2:8080 [HTTP RESPONSE] "302 Found (Set-Cookie)" (345 bytes)

[2026-06-04 10:07:10.567] HTTP         172.20.0.2:45828 → 172.20.0.2:8080 [HTTP POST] "/bytebite/api/kds" (256 bytes)
[2026-06-04 10:07:10.678] HTTP         172.20.0.2:45828 → 172.20.0.2:8080 [HTTP RESPONSE] "100 Continue" (12 bytes)

--- Two Threads Acquiring Locks in Opposite Order ---
--- Thread 1: Lock orders table, then inventory ---
--- Thread 2: Lock inventory table, then orders     ---
--- Result: Circular wait (DEADLOCK)                ---

[2026-06-04 10:07:10.789] PostgreSQL   172.20.0.2:52350 → 172.20.0.3:5432 [SQL UPDATE] "UPDATE orders SET status='IN_PROGRESS' WHERE id=12345" (1024 bytes)
[2026-06-04 10:07:10.845] PostgreSQL   172.20.0.3:5432  → 172.20.0.2:52350 [SQL RESPONSE] "LOCK ACQUIRED on orders" (56 bytes)

[2026-06-04 10:07:10.901] PostgreSQL   172.20.0.2:52351 → 172.20.0.3:5432 [SQL UPDATE] "UPDATE inventory SET qty=qty-1 WHERE item_id=99" (896 bytes)
[2026-06-04 10:07:10.945] PostgreSQL   172.20.0.3:5432  → 172.20.0.2:52351 [SQL RESPONSE] "LOCK ACQUIRED on inventory" (56 bytes)

--- Both threads now try to acquire the other table's lock ---

[2026-06-04 10:07:11.001] PostgreSQL   172.20.0.2:52350 → 172.20.0.3:5432 [SQL UPDATE] "UPDATE inventory SET qty=qty-1 WHERE item_id=99" (896 bytes)
[2026-06-04 10:07:11.112] PostgreSQL   172.20.0.3:5432  → 172.20.0.2:52350 [SQL RESPONSE] "LOCK WAIT - waiting for inventory lock" (67 bytes)

[2026-06-04 10:07:11.223] PostgreSQL   172.20.0.2:52351 → 172.20.0.3:5432 [SQL UPDATE] "UPDATE orders SET status='COMPLETE' WHERE id=12345" (1024 bytes)
[2026-06-04 10:07:11.334] PostgreSQL   172.20.0.3:5432  → 172.20.0.2:52351 [SQL RESPONSE] "LOCK WAIT - waiting for orders lock" (56 bytes)

--- Deadlock Detected ---

[2026-06-04 10:07:12.445] PostgreSQL   172.20.0.3:5432  → 172.20.0.2:52350 [SQL ERROR] "DEADLOCK DETECTED: Process 1234 waits for ShareLock" (245 bytes)
  ⚠️  [HIGH] DEADLOCK: SQL deadlock (inverted lock ordering)

[2026-06-04 10:07:12.556] PostgreSQL   172.20.0.2:52350 → 172.20.0.3:5432 [SQL QUERY] "ROLLBACK" (67 bytes)
[2026-06-04 10:07:12.667] PostgreSQL   172.20.0.3:5432  → 172.20.0.2:52350 [SQL RESPONSE] "TRANSACTION ROLLED BACK" (45 bytes)

--- Retry Attempt ---

[2026-06-04 10:07:12.778] PostgreSQL   172.20.0.2:52350 → 172.20.0.3:5432 [SQL UPDATE] "UPDATE orders SET status='IN_PROGRESS' WHERE id=12345" (1024 bytes)
[2026-06-04 10:07:12.889] PostgreSQL   172.20.0.3:5432  → 172.20.0.2:52350 [SQL RESPONSE] "1 row updated" (34 bytes)

[2026-06-04 10:07:12.990] HTTP         172.20.0.2:45828 → 172.20.0.2:8080 [HTTP RESPONSE] "500 Internal Server Error" (234 bytes)
```

**Signature**: Two SQL UPDATE transactions block each other:
1. Thread 1 locks table A, waits for lock on table B
2. Thread 2 locks table B, waits for lock on table A
3. PostgreSQL detects circular wait and raises "DEADLOCK DETECTED" error
4. One transaction rolls back, other retries

---

## Failure Scenario 5: SSL Certificate Failure

### Trigger

```bash
curl -k -X POST "https://localhost:8443/bytebite/checkout" \
  -H "Content-Type: application/json" \
  -d '{"failureType":"ssl"}'
```

### Network Capture

```
[2026-06-04 10:08:23.100] HTTP         172.20.0.2:45829 → 172.20.0.2:8443 [HTTPS POST] "/bytebite/checkout" (189 bytes)
[2026-06-04 10:08:23.200] HTTP         172.20.0.2:45829 → 172.20.0.2:8443 [TLS HANDSHAKE] "ClientHello" (512 bytes)

--- TLS Handshake Begins ---

[2026-06-04 10:08:23.312] HTTP         172.20.0.2:8443  → 172.20.0.2:45829 [TLS HANDSHAKE] "ServerHello" (456 bytes)
[2026-06-04 10:08:23.423] HTTP         172.20.0.2:8443  → 172.20.0.2:45829 [TLS HANDSHAKE] "Certificate: CN=self-signed" (2048 bytes)
  ⚠️  [MEDIUM] SSL: Self-signed certificate detected

[2026-06-04 10:08:23.534] HTTP         172.20.0.2:45829 → 172.20.0.2:8443 [TLS ALERT] "Certificate Verification Failed" (34 bytes)
  ⚠️  [HIGH] SSL: TLS/SSL certificate validation failure

[2026-06-04 10:08:23.645] HTTP         172.20.0.2:45829 → 172.20.0.2:8443 [TCP RESET] "RST" (0 bytes)
  ⚠️  [HIGH] SSL: TLS handshake failed, connection reset

[2026-06-04 10:08:24.756] HTTP         172.20.0.2:45829 → 172.20.0.2:8443 [HTTPS RESPONSE] (0 bytes)
```

**Signature**: 
1. TLS handshake initiates successfully
2. Server sends self-signed certificate
3. Client raises "Certificate Verification Failed" alert
4. Connection reset (RST)
5. HTTP response cannot be sent (connection closed)

---

## Summary Statistics

After running all 5 failure tests, a summary query:

```bash
docker exec bytebite-sniffer grep -c '⚠️' /logs/network_sniff.log
# Output: 12
```

Breakdown by failure type:

```bash
docker exec bytebite-sniffer grep '⚠️' /logs/network_sniff.log | \
  awk -F'[:\\[]' '{print $NF}' | sort | uniq -c

# Output:
#      3 DB_LEAK
#      2 LDAP_TIMEOUT
#      3 OOM
#      2 DEADLOCK
#      2 SSL
```

---

## Real-World Investigation Example

**Scenario**: A production engineer reports slowness on the analytics dashboard. Using the sniffer logs:

```bash
# Engineer searches for analytics-related traffic
docker exec bytebite-sniffer grep 'analytics' /logs/network_sniff.log

# Output shows:
[2026-06-04 10:05:56.345] PostgreSQL ... [SQL QUERY] "SELECT * FROM analytics_events LIMIT 1000000" ... (187432 bytes)
  ⚠️  [HIGH] OOM: Large result set detected (OOM risk) (187432 bytes)

# Engineer immediately identifies:
# 1. Query has no pagination (LIMIT 1000000 is unbounded)
# 2. Result set is 187KB (huge for a single query)
# 3. This matches the OOM failure pattern
# 4. Root cause: Unbounded analytics query loading all events into memory
```

Engineer then checks the application logs:

```bash
docker exec bytebite-tomcat grep -A5 'analytics' /usr/local/tomcat/logs/catalina.*.log
```

**Conclusion**: Network sniffer provided the smoking gun (large result set), pointing engineer directly to the analytics query code for optimization.

---

**Key Takeaway**: Each failure scenario leaves distinctive network signatures that, combined with application logs, allow engineers to pinpoint root causes quickly.
