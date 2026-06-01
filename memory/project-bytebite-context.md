---
name: project-bytebite-context
description: "ByteBite Restaurant System — a dockerized Java/Tomcat troubleshooting environment for an engineering book, with 5 intentional failure injection vectors"
metadata: 
  node_type: memory
  type: project
  originSessionId: 4993df77-2ed9-4cfb-bcca-5daac1735259
---

ByteBite is an interactive troubleshooting lab for an advanced Java debugging book. It's **not a production system**—it's a controlled learning environment where senior engineers trigger engineered failures and analyze logs to diagnose root causes.

**Why:** The book teaches hands-on debugging: readers pull failure toggles in the UI, observe failures in `catalina.out`, and learn to read diagnostics (thread dumps, heap stats, lock traces, SSL errors). Each failure mimics real production patterns.

**How to apply:** 

**5 Engineered Failure Vectors:**
- `/menu` → DB Connection Leak: HikariCP pool exhaustion
- `/login` → LDAP Auth Failure: Directory service timeout/unavailable
- `/analytics` → OOM Heap Exhaustion: Unbounded event buffering
- `/kds` → Thread Deadlock: Circular lock (order service ↔ database)
- `/checkout` → SSL Handshake Failure: Certificate validation error

**Immutable Stack:** Temurin JDK 21 (in Docker), Tomcat 11, PostgreSQL 15-alpine, OpenLDAP 1.5.0. Docker Compose on `bytebite-net`. UI is premium restaurant theme (vanilla HTML5 + CSS, no frameworks). All logs in `catalina.out` (SLF4J/Logback).

**Trigger Failures:** Pass `failureType` as HTTP request parameter to trigger individual failures (e.g., `?failureType=db_leak`). No global on/off switch—failures only activate when explicitly requested.
