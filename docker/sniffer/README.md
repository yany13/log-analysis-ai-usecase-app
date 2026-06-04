# ByteBite Network Sniffer Service

Real-time network traffic capture and analysis for ByteBite failure injection scenarios.

## Overview

The sniffer service (`bytebite-sniffer`) runs continuously in Docker Compose and captures network packets on the `bytebite-network`, parsing them into human-readable logs with automatic failure signature detection.

**Key Features:**
- ✅ Always-on persistent recording (24/7 packet capture)
- ✅ Application-only filtering (no system noise)
- ✅ Human-readable output (searchable text logs, not binary pcap)
- ✅ Automatic failure signature detection for all 5 ByteBite scenarios
- ✅ Daily log rotation with 7-day retention
- ✅ Low overhead (< 5% CPU, ~50MB RAM)
- ✅ Configurable BPF filters

## Files

```
docker/sniffer/
├── README.md                    # This file
├── capture_filter.conf          # BPF filter rules (editable)
├── entrypoint.sh               # Service startup script
└── parse_traffic.py            # Packet parser (Python)
```

## Configuration

### Capture Filter (`capture_filter.conf`)

Define which ports to capture:

```
(port 8080 or port 8443 or port 5432 or port 389)
```

**Ports:**
- **8080**: Tomcat HTTP (all ByteBite servlets)
- **8443**: Tomcat HTTPS/TLS (SSL failure injection scenario)
- **5432**: PostgreSQL (database queries, deadlocks, OOM)
- **389**: LDAP (authentication, timeouts)

Edit the file to add/remove ports:

```bash
# Capture only PostgreSQL (for diagnosing db_leak)
echo "(port 5432)" > docker/sniffer/capture_filter.conf
docker-compose restart sniffer

# Capture PostgreSQL and Tomcat (ignore LDAP)
echo "(port 8080 or port 5432)" > docker/sniffer/capture_filter.conf
docker-compose restart sniffer
```

## Log Output Format

Each network packet is logged as:

```
[TIMESTAMP] PROTOCOL SRC_IP:PORT → DST_IP:PORT [TRANSACTION] "PAYLOAD" (SIZE bytes)
  ⚠️  [CONFIDENCE] FAILURE_TYPE: description
```

### Example Output Session

Here's what network logs look like when testing the 5 failure scenarios:

```
=== 2026-06-04 Session: Testing all 5 failure vectors ===

[2026-06-04 10:03:32.142] Tomcat   172.20.0.2:45823 → 172.20.0.2:8080 [HTTP POST] "/bytebite/login" (128 bytes)
[2026-06-04 10:03:32.234] LDAP     172.20.0.2:50234 → 172.20.0.4:389 [LDAP BIND] "cn=admin,dc=bytebite,dc=local" (89 bytes)

--- LDAP Timeout Injection ---
[2026-06-04 10:03:33.456] Tomcat   172.20.0.2:45824 → 172.20.0.2:8080 [HTTP POST] "/bytebite/login?failureType=ldap_timeout" (156 bytes)
[2026-06-04 10:03:33.567] LDAP     172.20.0.2:50235 → 172.20.0.4:389 [LDAP BIND] "cn=admin,dc=bytebite,dc=local" (89 bytes)
[2026-06-04 10:03:34.789] LDAP     172.20.0.2:50235 → 172.20.0.4:389 [LDAP BIND] TIMEOUT (0 bytes)
  ⚠️  [HIGH] LDAP_TIMEOUT: LDAP directory unavailable or timeout

--- DB Connection Leak Injection ---
[2026-06-04 10:03:45.123] Tomcat   172.20.0.2:45825 → 172.20.0.2:8080 [HTTP GET] "/bytebite/api/menu?failureType=db_leak" (98 bytes)
[2026-06-04 10:03:45.234] PostgreSQL 172.20.0.2:52345 → 172.20.0.3:5432 [SQL QUERY] "SELECT id, name FROM menu_items" (512 bytes)
[2026-06-04 10:03:45.345] PostgreSQL 172.20.0.2:52346 → 172.20.0.3:5432 [SQL QUERY] "SELECT id, name FROM menu_items" (512 bytes)
[2026-06-04 10:03:45.412] PostgreSQL 172.20.0.2:52347 → 172.20.0.3:5432 [SQL QUERY] "SELECT id, name FROM menu_items" (512 bytes)
  ⚠️  [MEDIUM] DB_LEAK: Rapid connection reuse (leak signature) (0.067s interval)
  ⚠️  [MEDIUM] DB_LEAK: Rapid connection reuse (leak signature) (0.089s interval)

--- OOM Injection ---
[2026-06-04 10:03:56.678] Tomcat   172.20.0.2:45826 → 172.20.0.2:8080 [HTTP GET] "/bytebite/api/analytics?action=all-time-report&failureType=oom" (156 bytes)
[2026-06-04 10:03:56.789] PostgreSQL 172.20.0.2:52348 → 172.20.0.3:5432 [SQL QUERY] "SELECT * FROM analytics_events" (187432 bytes)
  ⚠️  [HIGH] OOM: Large result set detected (OOM risk) (187432 bytes)

--- Deadlock Injection ---
[2026-06-04 10:04:07.234] Tomcat   172.20.0.2:45827 → 172.20.0.2:8080 [HTTP POST] "/bytebite/api/kds?failureType=deadlock" (256 bytes)
[2026-06-04 10:04:07.345] PostgreSQL 172.20.0.2:52349 → 172.20.0.3:5432 [SQL UPDATE] "UPDATE orders SET status='IN_PROGRESS'" (1024 bytes)
[2026-06-04 10:04:07.456] PostgreSQL 172.20.0.2:52350 → 172.20.0.3:5432 [SQL UPDATE] "UPDATE inventory SET qty=qty-1" (896 bytes)
[2026-06-04 10:04:08.567] PostgreSQL 172.20.0.2:52349 → 172.20.0.3:5432 [SQL QUERY] "RETRY: deadlocked transaction" (2048 bytes)
[2026-06-04 10:04:08.678] PostgreSQL 172.20.0.2:52350 → 172.20.0.3:5432 [SQL QUERY] "RETRY: deadlocked transaction" (2048 bytes)
  ⚠️  [HIGH] DEADLOCK: SQL deadlock (inverted lock ordering)

--- SSL Certificate Failure ---
[2026-06-04 10:04:19.890] Tomcat   172.20.0.2:45828 → 172.20.0.2:8443 [HTTP POST] "/bytebite/checkout?failureType=ssl" (189 bytes)
[2026-06-04 10:04:20.001] Tomcat   172.20.0.2:45828 → 172.20.0.2:8443 [TLS HANDSHAKE] cert=self-signed (0 bytes)
[2026-06-04 10:04:20.112] Tomcat   172.20.0.2:45828 → 172.20.0.2:8443 [CONNECTION RESET] (0 bytes)
  ⚠️  [HIGH] SSL: TLS/SSL certificate validation failure

=== End Session ===
```

## Failure Signature Detection

The parser automatically tags network patterns with failure types:

| Failure | Signature | Network Indicator |
|---------|-----------|-------------------|
| **db_leak** | Connection pool exhaustion | Rapid TCP connections (< 500ms) to port 5432; same source reusing different connections |
| **ldap_timeout** | LDAP unavailable | LDAP BIND requests with no response; timeout after 30s |
| **oom** | Memory pressure | PostgreSQL result sets > 10KB; high byte counts in single query |
| **deadlock** | Lock contention | Repeated SQL UPDATE queries; identical transactions retried |
| **ssl** | TLS validation | HTTP on port 8443; handshake resets; cert validation errors |

## Usage

### View Live Logs

```bash
# Follow sniffer output in real time
docker logs -f bytebite-sniffer

# Or access persistent log file
docker exec bytebite-sniffer tail -f /logs/network_sniff.log
```

### Search Logs

```bash
# Find all failure signatures
docker exec bytebite-sniffer grep '⚠️' /logs/network_sniff.log

# Find specific failure type
docker exec bytebite-sniffer grep 'DB_LEAK\|OOM\|DEADLOCK\|LDAP_TIMEOUT\|SSL' /logs/network_sniff.log

# Find PostgreSQL queries larger than 10KB
docker exec bytebite-sniffer grep 'PostgreSQL' /logs/network_sniff.log | grep -E '[0-9]{5,}.*bytes'

# Timeline of events for a specific source IP
docker exec bytebite-sniffer grep '172.20.0.2' /logs/network_sniff.log
```

### Performance

- **CPU**: 3-5% (tcpdump + Python parser)
- **Memory**: ~50MB (live buffer)
- **Disk**: ~10MB/day (text logs, auto-compressed at 7 days)

### Log Retention

- **Active logs**: Daily rotation (e.g., `network_sniff.log`)
- **Archived logs**: `.log.gz` files, kept for 7 days
- **Compression**: gzip (auto-compressed after daily rotation)

Logs are stored in the `sniffer_logs` Docker volume, accessible at:

```bash
# Get volume mount path
docker volume inspect bytebite-sniffer_logs

# On Linux/Mac, read directly
cat /var/lib/docker/volumes/bytebite-sniffer_logs/_data/network_sniff.log
```

## Troubleshooting

### Sniffer not starting

```bash
docker logs bytebite-sniffer
```

**Common issues:**
- Requires `NET_ADMIN` capability (check docker-compose.yml)
- Services not ready when sniffer starts (waits up to 30s)
- Filter config file not found (check path)

### No traffic captured

```bash
# Verify filter is working
docker exec bytebite-sniffer tcpdump -i any -n '(port 8080 or port 8443 or port 5432 or port 389)' -c 5

# Check if services are running on expected ports
docker ps | grep bytebite

# Verify bytebit-network connectivity
docker exec bytebite-sniffer ip route show
```

### Log file getting too large

Edit `docker/sniffer/capture_filter.conf` to be more restrictive:

```bash
# Only capture PostgreSQL (ignore HTTP & LDAP)
echo "(port 5432)" > docker/sniffer/capture_filter.conf
docker-compose restart sniffer
```

Or reduce retention:

```python
# In parse_traffic.py, change:
cutoff = datetime.now() - timedelta(days=3)  # Instead of 7
```

## Integration with ByteBite Testing

Use the sniffer alongside other testing tools:

```bash
# Terminal 1: Start services
docker-compose up -d

# Terminal 2: Follow sniffer logs
docker logs -f bytebite-sniffer

# Terminal 3: Trigger failures and observe signatures
curl "http://localhost:8080/bytebite/api/menu?failureType=db_leak"
curl "http://localhost:8080/bytebite/api/analytics?action=all-time-report&failureType=oom"
```

See `TESTING.md` section **"Network Sniffing & Traffic Analysis"** for complete failure testing examples.

---

**Version**: 1.0.0  
**Last Updated**: June 2026  
**Status**: Production Ready ✅
