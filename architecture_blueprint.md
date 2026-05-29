# Blueprint: ByteBite Restaurant System Architecture

You are an Enterprise Solution Architect and CTO. This document defines the core architecture and precise software versions for a local Java/Tomcat troubleshooting environment to be featured in our engineering book.

## 1. Host Infrastructure & Deployment Strategy
All components must run on a single local developer workstation managed by a unified `docker-compose.yml` file.

### Physical Software Stack & Explicit Versions:
* **Application Runtime:** Apache Tomcat v10.1.x
* **Java Development Kit:** Eclipse Temurin JDK 17 (LTS)
* **Database Engine:** PostgreSQL v18.x (Official Alpine-based image)
* **Directory Service (IAM):** OpenLDAP v2.6.x (via `osixia/openldap:1.5.0` wrapper)

## 2. Component Layout & Network Topology
* **Network:** Single isolated Docker bridge network (`bytebite-net`).
* **Tomcat Service:** Exposes port `8080` (HTTP) and `8443` (HTTPS) to the host. Contains the single `ByteBiteApp.war` file.
* **PostgreSQL Service:** Exposes port `5432` internally to the Tomcat container only.
* **OpenLDAP Service:** Exposes ports `389` (LDAP) and `636` (LDAPS) internally to the Tomcat container.

## 3. Core Database Schema & Static Data
To maintain simplicity, the database utilizes a single schema with three minimal tables:
1.  `users` (id, username, role)
2.  `menu_items` (id, name, price, category, inventory_count)
3.  `orders` (id, user_id, total_amount, status, timestamp)

## 4. Log Delivery Target
All standard output and Java stack traces must route natively to Tomcat's log stream.
* **File Pathway:** Inside container: `/usr/local/tomcat/logs/catalina.out`
* **Log Objective:** Must be written using standard Java logging patterns without custom obfuscation, optimizing readability for both human engineers and downstream automated LLM log-parsing agents.