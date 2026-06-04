# ByteBite Network Sniffer - Quick Start

Get the sniffer running and viewing network traffic in 5 minutes.

## 1. Start Services

```bash
cd /path/to/bytebite
docker-compose up -d
sleep 45  # Wait for health checks
```

## 2. Verify Sniffer is Running

```bash
docker-compose ps | grep sniffer
# Output: bytebite-sniffer ... running

docker logs bytebite-sniffer
# Output: [2026-06-04 10:00:00...] ByteBite Network Sniffer started
```

## 3. View Live Network Traffic

```bash
docker logs -f bytebite-sniffer
```

Output format:
```
[TIMESTAMP] PROTOCOL SRC:PORT → DST:PORT [TRANSACTION] "PAYLOAD" (SIZE bytes)
```

Example:
```
[2026-06-04 10:03:32.142] PostgreSQL 172.20.0.2:45823 → 172.20.0.3:5432 [SQL QUERY] "SELECT * FROM menu_items" (512 bytes)
```

## 4. Trigger a Failure & Watch Signature

In another terminal:

```bash
curl "http://localhost:8080/bytebite/api/menu?failureType=db_leak"
```

Watch sniffer logs for the failure signature:

```
[2026-06-04 10:04:45.312] PostgreSQL 172.20.0.2:52345 → 172.20.0.3:5432 [SQL QUERY] ...
[2026-06-04 10:04:45.378] PostgreSQL 172.20.0.2:52346 → 172.20.0.3:5432 [SQL QUERY] ...
  ⚠️  [MEDIUM] DB_LEAK: Rapid connection reuse (leak signature) (0.066s interval)
```

## 5. Search Logs for Failures

```bash
# Find all detected failures
docker exec bytebite-sniffer grep '⚠️' /logs/network_sniff.log

# Find specific failure type
docker exec bytebite-sniffer grep 'DB_LEAK\|OOM\|DEADLOCK' /logs/network_sniff.log

# Monitor in real time
docker exec bytebite-sniffer tail -f /logs/network_sniff.log | grep -E 'PostgreSQL|LDAP|⚠️'
```

## 6. Common Tasks

### Test All 5 Failures

```bash
# DB Leak
curl "http://localhost:8080/bytebite/api/menu?failureType=db_leak"

# LDAP Timeout
curl -X POST "http://localhost:8080/bytebite/login?failureType=ldap_timeout" \
  -d "username=admin&password=admin"

# OOM
curl "http://localhost:8080/bytebite/api/analytics?action=all-time-report&failureType=oom"

# Deadlock (requires auth)
curl -c /tmp/cookies.txt -X POST "http://localhost:8080/bytebite/login" \
  -d "username=admin&password=admin"
curl -b /tmp/cookies.txt -X POST "http://localhost:8080/bytebite/api/kds" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-1", "status":"IN_PROGRESS", "failureType":"deadlock"}'

# SSL (requires HTTPS)
curl -k -X POST "https://localhost:8443/bytebite/checkout" \
  -H "Content-Type: application/json" \
  -d '{"failureType":"ssl"}'
```

### Adjust Filter

Edit `docker/sniffer/capture_filter.conf`:

```bash
# Only PostgreSQL (ignore HTTP and LDAP)
echo "(port 5432)" > docker/sniffer/capture_filter.conf

# Restart sniffer
docker-compose restart sniffer
```

### View Log File

```bash
# Persistent logs (survives restarts)
docker exec bytebite-sniffer ls -lah /logs/

# Download logs to host
docker cp bytebite-sniffer:/logs/network_sniff.log ./sniffer_logs.txt
```

## Example Session Output

```
# User Login
[2026-06-04 10:03:32.234] LDAP       172.20.0.2:50234 → 172.20.0.4:389 [LDAP BIND] (89 bytes)
[2026-06-04 10:03:32.345] HTTP       172.20.0.2:45823 → 172.20.0.2:8080 [HTTP RESPONSE] (234 bytes)

# Trigger DB Leak
[2026-06-04 10:04:45.312] PostgreSQL 172.20.0.2:52345 → 172.20.0.3:5432 [SQL QUERY] ... (512 bytes)
[2026-06-04 10:04:45.378] PostgreSQL 172.20.0.2:52346 → 172.20.0.3:5432 [SQL QUERY] ... (512 bytes)
  ⚠️  [MEDIUM] DB_LEAK: Rapid connection reuse (leak signature) (0.066s interval)

# Trigger OOM
[2026-06-04 10:05:56.345] PostgreSQL 172.20.0.2:52349 → 172.20.0.3:5432 [SQL QUERY] ... (187432 bytes)
  ⚠️  [HIGH] OOM: Large result set detected (OOM risk) (187432 bytes)
```

## Troubleshooting

**Sniffer not running?**
```bash
docker logs bytebite-sniffer
```

**No traffic being captured?**
```bash
# Test filter
docker exec bytebite-sniffer tcpdump -i any -n '(port 8080 or port 5432)' -c 5

# Check services are ready
docker-compose ps
```

**Log file too large?**
```bash
# Edit filter to capture less
echo "(port 5432)" > docker/sniffer/capture_filter.conf
docker-compose restart sniffer
```

## Next Steps

- See [README.md](README.md) for full configuration details
- See [EXAMPLE_OUTPUT.md](EXAMPLE_OUTPUT.md) for real-world capture examples
- See [TESTING.md](../TESTING.md#network-sniffing--traffic-analysis) for advanced usage

---

**Need help?** Check the main [TESTING.md](../TESTING.md#network-sniffing--traffic-analysis) "Network Sniffing" section.
