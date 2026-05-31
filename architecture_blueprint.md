# Blueprint: ByteBite Restaurant System Architecture

You are an Enterprise Solution Architect and CTO. This document defines the core architecture and precise software versions for a local Java/Tomcat troubleshooting environment to be featured in our engineering book.

## 1. Host Infrastructure & Deployment Strategy
All components must run on a single local developer workstation managed by a unified `docker-compose.yml` file.

### Physical Software Stack & Explicit Versions:
* **Application Runtime:** Apache Tomcat v11 (JDK 21 Temurin)
* **Java Development Kit:** Eclipse Temurin JDK 21 (LTS)
* **Database Engine:** PostgreSQL v15-alpine (Official Alpine-based image)
* **Directory Service (IAM):** OpenLDAP v1.5.0 (via `osixia/openldap:1.5.0`)

## 2. Component Layout & Network Topology
* **Network:** Single isolated Docker bridge network (`bytebite-net`).
* **Tomcat Service:** Exposes port `8080` (HTTP) and `8443` (HTTPS) to the host. Contains the single `ByteBiteApp.war` file.
* **PostgreSQL Service:** Exposes port `5432` internally to the Tomcat container only.
* **OpenLDAP Service:** Exposes ports `389` (LDAP) and `636` (LDAPS) internally to the Tomcat container.

## 3. Core Database Schema & Static Data
The database utilizes a single schema (`bytebite`) with eleven production-grade tables:
1.  `users` - User profiles and authentication
2.  `user_roles` - Role definitions
3.  `user_role_mapping` - Role assignments
4.  `sessions` - Session management
5.  `orders` - Order information
6.  `order_items` - Order line items
7.  `kds_orders` - Kitchen display orders
8.  `analytics_events` - Event tracking (JSONB)
9.  `api_audit_log` - API call logging
10. `application_logs` - Application events
11. Additional tables indexed on common query columns

## 4. Log Delivery Target
All standard output and Java stack traces must route natively to Tomcat's log stream.
* **File Pathway:** Inside container: `/usr/local/tomcat/logs/catalina.out`
* **Log Objective:** Must be written using standard Java logging patterns without custom obfuscation, optimizing readability for both human engineers and downstream automated LLM log-parsing agents.