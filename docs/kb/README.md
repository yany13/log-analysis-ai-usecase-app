# ByteBite Failure Use Cases - Knowledge Base

This directory contains comprehensive documentation for all 5 engineered failure scenarios in the ByteBite troubleshooting lab.

## 📋 Documents

Each failure scenario has a dedicated Word document with:
- **Failure type** and trigger mechanism
- **Root cause analysis** with code patterns
- **Log signatures** you'll see in production
- **Diagnostic approach** step-by-step
- **Exact solutions** with code examples
- **How to trigger locally** for hands-on practice
- **Prevention strategies** to avoid in production

### Failure Scenarios

| # | Document | Failure Type | Endpoint | Trigger |
|---|----------|--------------|----------|---------|
| 1 | [01-DB-Connection-Pool-Exhaustion.docx](01-DB-Connection-Pool-Exhaustion.docx) | `db_leak` | `GET /api/menu` | `?failureType=db_leak` |
| 2 | [02-LDAP-Authentication-Timeout.docx](02-LDAP-Authentication-Timeout.docx) | `ldap_timeout` | `POST /login` | `?failureType=ldap_timeout` |
| 3 | [03-Out-of-Memory.docx](03-Out-of-Memory.docx) | `oom` | `GET /api/analytics` | `&failureType=oom` |
| 4 | [04-Thread-Deadlock.docx](04-Thread-Deadlock.docx) | `deadlock` | `POST /api/kds` | JSON `{"failureType":"deadlock"}` |
| 5 | [05-SSL-Certificate-Validation.docx](05-SSL-Certificate-Validation.docx) | `ssl` | `POST /checkout` | JSON `{"failureType":"ssl"}` |

---

## 🎯 How to Use This Knowledge Base

### For Learning the Failures
1. Start with the failure you're investigating
2. Read the **Root Cause** section to understand the pattern
3. Look at the **Log Signatures** to know what to search for
4. Follow the **Diagnostic Approach** step-by-step
5. Review the **Solution** to understand the fix

### For Practicing Troubleshooting
1. Open the **How to Trigger Locally** section
2. Follow the exact curl commands to reproduce the failure
3. Check logs while the failure is active
4. Confirm the log signatures match what's documented
5. Use the **Diagnostic Approach** to locate the issue
6. Verify your solution against the documented fix

### For Production Incident Response
1. When facing a production issue, identify the failure pattern
2. Search logs for the exact **Log Signatures**
3. Follow the **Diagnostic Approach** to confirm root cause
4. Apply the documented **Solution**
5. Review **Prevention** strategies to prevent recurrence

---

## 📚 Related Documents

- [Chapter 2: Understanding ByteBite](../02-understanding-bytebite.md) — System architecture and overview
- [Chapter 4: Connection Pool Exhaustion](../04-connection-pool-exhaustion.md) — Detailed walkthrough of failure #1
- [CLAUDE.md](../../CLAUDE.md) — Project specifications and constraints

---

## 🔍 Quick Reference: Log Patterns

### Failure #1: DB Connection Pool
```
⚠️ Connection leak scenario
❌ CONNECTION LEAK: Connection not closed
Connection is not available, request timed out after 30000ms
```

### Failure #2: LDAP Timeout
```
⚠️ LDAP Failure Injection: Simulating connection timeout
CommunicationException: openldap:389: Connection timed out
❌ LDAP AUTHENTICATION FAILED
```

### Failure #3: Out of Memory
```
⚠️ OOM FAILURE INJECTION: Loading all-time analytics without pagination
OutOfMemoryError: Java heap space
❌ OOM FAILURE SCENARIO TRIGGERED
```

### Failure #4: Deadlock
```
⚠️ DEADLOCK FAILURE INJECTION: Creating deadlock scenario
❌ DEADLOCK SCENARIO: Both threads are now BLOCKED
Thread-1: Attempting to acquire inventoryLock (blocked)
```

### Failure #5: SSL Certificate
```
⚠️ SSL FAILURE INJECTION: Attempting connection with self-signed certificate
❌ SSL CERTIFICATE VALIDATION FAILED
sun.security.validator.ValidatorException: PKIX path building failed
```

---

## 🛠️ Testing Commands

### Start ByteBite
```bash
cd D:\workspace\ai\code\log-analysis-ai-usecase-app
docker-compose up -d
sleep 45
```

### Test Failure #1: DB Leak
```bash
curl "http://localhost:8080/bytebite/api/menu?failureType=db_leak"
```

### Test Failure #2: LDAP Timeout
```bash
curl -X POST "http://localhost:8080/bytebite/login?failureType=ldap_timeout" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin", "password":"admin"}'
```

### Test Failure #3: OOM (requires auth)
```bash
# First, authenticate
curl -c /tmp/cookies.txt -X POST "http://localhost:8080/bytebite/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin", "password":"admin"}'

# Then trigger OOM
curl -b /tmp/cookies.txt \
  "http://localhost:8080/bytebite/api/analytics?action=all-time-report&failureType=oom"
```

### Test Failure #4: Deadlock (requires auth)
```bash
curl -b /tmp/cookies.txt -X POST "http://localhost:8080/bytebite/api/kds" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-1", "status":"COMPLETED", "failureType":"deadlock"}'
```

### Test Failure #5: SSL (requires auth)
```bash
curl -b /tmp/cookies.txt -X POST "http://localhost:8080/bytebite/checkout" \
  -H "Content-Type: application/json" \
  -d '{"items":[...], "failureType":"ssl"}'
```

### Monitor Logs
```bash
docker-compose logs -f tomcat | grep -E "FAILURE|DEADLOCK|Connection leak|OOM|SSL"
```

---

## 📖 Document Metadata

- **Generated**: June 2026
- **Format**: Microsoft Word (.docx)
- **Target Audience**: Senior Java engineers, DevOps professionals
- **Content Style**: Architecture-first, concise, code examples included
- **Tone**: Peer-to-peer, direct and authoritative

---

**These documents are part of the ByteBite troubleshooting lab used in "AI Agents for Production Troubleshooting" book.**
