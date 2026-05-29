---
name: project-bytebite-context
description: "ByteBite Restaurant System — a dockerized Java/Tomcat troubleshooting environment for an engineering book, with 5 intentional failure injection vectors"
metadata: 
  node_type: memory
  type: project
  originSessionId: 4993df77-2ed9-4cfb-bcca-5daac1735259
---

ByteBite is a local Java troubleshooting lab for an engineering book teaching senior engineers how to debug production failures.

**Why:** The book's premise is hands-on: readers trigger real failures in a controlled environment and analyze `catalina.out` logs to diagnose them.

**How to apply:** All code must contain explicit, intentional bugs togglable via the UI. The five vectors are: DB connection leak (/menu), LDAP auth failure (/login), OOM heap exhaustion (/analytics), thread deadlock (/kds), SSL handshake failure (/checkout). Stack is immutable: Temurin JDK 17, Tomcat 10.1, PostgreSQL 18, OpenLDAP 2.6. Docker Compose on `bytebite-net`. UI is premium restaurant theme, vanilla HTML5 only.
