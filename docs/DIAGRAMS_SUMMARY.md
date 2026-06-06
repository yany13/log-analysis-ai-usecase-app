# ByteBite Book Diagrams — Visual Reference Guide (Version 3.0)

## Overview

**9 professional, publication-ready architecture diagrams** have been created for the ByteBite troubleshooting guide:

### 📊 Original Overview Diagrams (1-4)
- **Infrastructure topology** (user to database flow)
- **Application request flows** (login, menu, analytics, checkout, KDS)
- **Database schema architecture** (11 tables and relationships)
- **Servlet-to-database connection patterns** (HikariCP, all 5 failure injection points side-by-side)

### 📘 New: Individual Use-Case Diagrams (5A-5E)
- **One diagram per failure type**, optimized for A4 print and book chapters
- **Detailed execution flows** with numbered steps and diagnostic signals
- Each diagram shows: trigger, execution path, failure point, log signatures, troubleshooting tips

All diagrams are **code-free**, **SVG-based** for scalability, and suitable for book publication. They show failure patterns and diagnostic signatures readers will encounter in logs.

---

## 📊 Diagram Files

### 1. **Infrastructure Architecture** (`01-infrastructure-architecture.svg`)
**File**: `docs/01-infrastructure-architecture.svg`

**Shows**:
- User browser → HTTP/HTTPS entry points (8080, 8443)
- Apache Tomcat 11 with 6 servlets + 4 filters
- Request pipeline: RequestLogging → LDAP Auth → CORS → Servlet Router → Failure Injection
- OpenLDAP 1.5.0 for authentication (port 389)
- PostgreSQL 15 with HikariCP connection pool (max 20 connections)
- SLF4J/JUL logging with daily rotation
- Docker Compose network topology (bytebite-net)

**Key Talking Points**:
- Three-tier architecture: User → Middleware → Data
- Daily log rotation: `catalina.YYYY-MM-DD.log` with 90-day retention
- Connection pooling: HikariCP max 20 connections for PostgreSQL
- Async logging: SLF4J → JUL handler → async file writer

**Use in Book**: Chapter 1 - System Architecture & Component Overview

---

### 2. **Application Request & Session Flow** (`02-application-flow.svg`)
**File**: `docs/02-application-flow.svg`

**Shows**:
- Complete HTTP filter chain with 5 stages
- 5 major flows with database interactions:
  1. **LOGIN** (POST /login): LDAP bind + session creation — `LDAP_TIMEOUT` injection point
  2. **MENU** (GET /api/menu): Query menu_items + analytics logging — `DB_LEAK` injection point
  3. **ANALYTICS** (GET /api/analytics?all-time-report): Load ~10K events → `OOM` injection point
  4. **KDS** (POST /api/kds): Update order status with locks — `DEADLOCK` injection point
  5. **CHECKOUT** (POST /checkout): Multi-table transaction — `SSL` / `DEADLOCK` injection points
- Session lifecycle: created → validated → refreshed → expired
- Session states with 30-minute timeout
- Four transaction patterns: Query, Multi-step, Lock, Leak

**Key Talking Points**:
- Filter chain pattern: Security & auth happen before servlet handler
- Session management: 30-minute expiration with validation on each request
- Six servlets: Each handles one business flow
- Failure injection: `?failureType` bypasses auth for chaos testing

**Use in Book**: Chapters 2-6 - Request Flow Tracing & Failure Troubleshooting

---

### 3. **Database Schema Architecture** (`03-database-schema.svg`)
**File**: `docs/03-database-schema.svg`

**Shows**:
- All **11 tables** organized by category:
  - **Authentication** (4): users, user_roles, user_role_mapping, sessions
  - **Transactions** (4): orders, order_items, kds_orders, menu_items
  - **Analytics/Logging** (3): analytics_events, application_logs, api_audit_log
- Table details: columns, data types, indices, constraints
- Foreign key relationships (1:N, M:M junctions)
- Pre-seeded data (16 menu items, 5 roles, ~10K analytics events)
- Key failure points marked on relevant tables:
  - `DB_LEAK` point: menu_items table query
  - `OOM` point: analytics_events table (10K rows × 100KB each)

**Pre-seeded Data Examples**:
- Menu items: 3 pizzas, 3 burgers, 3 pastas, 2 salads, 3 desserts, 2 beverages
- Roles: ADMIN, MANAGER, STAFF, CUSTOMER, KDS_OPERATOR
- Analytics: ~10,000 events for testing unbounded queries

**Key Talking Points**:
- Eleven tables: Four for users/auth, four for transactions, three for logging
- JSONB for flexibility: `analytics_events.event_data` stores arbitrary event shapes
- Pre-seeded test data: Enables immediate failure testing
- Composite keys: `user_role_mapping` uses (user_id, role_id) for M:M

**Use in Book**: Chapter 3 - Data Model Understanding & Schema Relationships

---

### 4. **Servlet & Database Connection Architecture (Overview)** (`04-servlet-architecture.svg`)
**File**: `docs/04-servlet-architecture.svg`

**Shows**:
- **All 5 flows together** showing servlet → service → DAO → HikariCP → SQL
- **HikariCP pool state**: idle, active, waiting, exhausted
- **PreparedStatement execution**: SQL injection prevention
- **Connection lifecycle**: getConnection() → execute → close() → back to pool
- **Failure Summary Table**: All 5 failure types with triggers, root causes, log signals
- Side-by-side view of all flows for reference and comparison

**Key Talking Points**:
- PreparedStatements: All SQL uses parameterized queries (SQL injection protection)
- Connection lifecycle: getConnection() → execute → close() → back to pool
- Failure injection points: Marked at specific code locations where failures activate
- Thread safety: HikariCP is thread-safe; deadlock occurs at DB level

**Use in Book**: Chapter 1 - Introduction to the Architecture (high-level reference showing all 5 flows at once)

---

## 📘 Use Case Focused Diagrams (Individual Deep Dives)

Each failure type has a dedicated **A4-optimized diagram** for detailed troubleshooting chapters. These are expanded versions of flows from Diagram 4, with:
- **Larger, clearer typography** for print publication
- **Detailed execution steps** (numbered ①-⑦ or more)
- **Diagnostic signal boxes** showing exact log patterns to look for
- **Pool/memory/lock state visualizations** specific to each failure
- **Best practices for diagnosis** at the bottom of each diagram

### 5A. **Use Case 1: LDAP Timeout Failure** (`05-servlet-architecture-01-login-ldap-timeout.svg`)
**File**: `docs/05-servlet-architecture-01-login-ldap-timeout.svg`

**Shows**:
- **Flow**: HTTP Request → Filter Chain → LoginServlet → UserService → LDAP Timeout
- **Failure Point**: Line 88 in LoginServlet.java where `failureType == "ldap_timeout"`
- **Trigger**: `POST /login?failureType=ldap_timeout` with credentials
- **What Fails**: `SocketTimeoutException` during LDAP bind operation
- **Log Signals**: 
  - `"⚠️ LDAP Failure Injection: Simulating connection timeout"`
  - `"SocketTimeoutException"` or `"java.net.ConnectException"` in stack trace
- **Diagnostic Tips**: Check LDAP server health, network connectivity to port 389

**Use in Book**: Chapter 4 - External Service Timeout Failures (LDAP Authentication)

---

### 5B. **Use Case 2: Database Connection Leak** (`05-servlet-architecture-02-menu-db-leak.svg`)
**File**: `docs/05-servlet-architecture-02-menu-db-leak.svg`

**Shows**:
- **Flow**: HTTP Request → MenuServlet → MenuService → MenuDAO (HikariCP pool)
- **Failure Point**: Line 79 in MenuServlet.java where `failureType == "db_leak"`
- **Trigger**: `GET /api/menu?failureType=db_leak`
- **What Fails**: Connection is retrieved but never returned to pool (`conn.close()` skipped)
- **HikariCP Timeline**: 
  - Requests 1-20: Slow (connections not returned)
  - Request 21+: Immediate timeout (pool exhausted, max=20)
- **Log Signals**:
  - `"Cannot get a connection, pool error..."`
  - `"⚠️ Connection leak scenario: connection not closed"`
  - HikariCP shows 20 active connections, 0 idle
- **Diagnostic Tips**: Monitor HikariCP metrics, count active connections, check for missing `close()` calls

**Use in Book**: Chapter 5 - Resource Leaks & Connection Pool Exhaustion

---

### 5C. **Use Case 3: Out-of-Memory (OOM) Heap Exhaustion** (`05-servlet-architecture-03-analytics-oom.svg`)
**File**: `docs/05-servlet-architecture-03-analytics-oom.svg`

**Shows**:
- **Flow**: HTTP Request → AnalyticsServlet → AnalyticsService → AnalyticsDAO (all-time query)
- **Failure Point**: Line 96 in AnalyticsServlet.java where `failureType == "oom"`
- **Trigger**: `GET /api/analytics?action=all-time-report&failureType=oom`
- **What Fails**: Unbounded SELECT query returns 10K events (100-150 bytes each = 1-1.5MB) into memory
- **Heap Timeline**:
  - Idle: 100MB
  - 1K events: 200MB
  - 5K events: 350MB
  - 10K events: **512MB (MAX)** → OutOfMemoryError
- **Log Signals**:
  - `"⚠️ OOM FAILURE INJECTION: Loading 10K events"`
  - `"java.lang.OutOfMemoryError: Java heap space"`
  - GC logs: `"[Full GC... freed NMB, X% memory]"` (multiple rapid cycles)
- **Diagnostic Tips**: Monitor heap usage spikes, add pagination/cursor limits to queries, use jstack for thread dumps

**Use in Book**: Chapter 6 - Unbounded Query & Heap Management

---

### 5D. **Use Case 4: Database Deadlock** (`05-servlet-architecture-04-kds-deadlock.svg`)
**File**: `docs/05-servlet-architecture-04-kds-deadlock.svg`

**Shows**:
- **Concurrent Flows**:
  - **Transaction 1** (KDS): Locks kds_orders → waits for orders lock
  - **Transaction 2** (Checkout): Locks orders → waits for kds_orders lock
- **Failure Point**: Lines 55 (KDSServlet) and 68 (CheckoutServlet) where `failureType == "deadlock"`
- **Trigger**: Concurrent POST requests to `/api/kds` and `/checkout` with `failureType: deadlock`
- **What Fails**: Circular lock dependency → PostgreSQL detects deadlock → **ROLLBACK one transaction**
- **Lock Dependency Graph**: Shows circular wait (Transaction 1 → Transaction 2 → Transaction 1)
- **Log Signals**:
  - `"❌ DEADLOCK SCENARIO: Both threads are now BLOCKED"`
  - `"org.postgresql.util.PSQLException: ERROR: deadlock detected"`
  - PostgreSQL logs: `"ERROR: deadlock detected"` with DETAIL section
- **Diagnostic Tips**: Review lock ordering in transaction code, use thread dumps to identify blocked threads, check PostgreSQL deadlock logs

**Use in Book**: Chapter 7 - Concurrent Transaction & Deadlock Detection

---

### 5E. **Use Case 5: SSL Certificate Validation Failure** (`05-servlet-architecture-05-checkout-ssl.svg`)
**File**: `docs/05-servlet-architecture-05-checkout-ssl.svg`

**Shows**:
- **Flow**: HTTP Request → CheckoutServlet → OrderService → OrderDAO (multi-table transaction) → SSL validation
- **Failure Point**: Line 68 in CheckoutServlet.java where `failureType == "ssl"`
- **Trigger**: `POST /checkout` with JSON payload + `failureType: ssl`
- **What Fails**: SSL certificate validation fails (self-signed cert) after partial inserts (orders, order_items)
- **Transaction Timeline**:
  - T=0ms: BEGIN TRANSACTION
  - T=100ms: INSERT into orders, order_items ✓
  - T=150ms: SSL validation fails → SSLHandshakeException
  - T=160ms: ROLLBACK COMPLETE (all rows deleted)
- **Log Signals**:
  - `"❌ SSL Certificate Validation Failed"`
  - `"javax.net.ssl.SSLHandshakeException: ... PKIX validation failed"`
  - `"ROLLBACK [orderId=ORD-XXX]"` (no rows persisted)
- **Diagnostic Tips**: Check TLS/SSL certificates, validate certificate chain, monitor for partial transaction failures in audit logs

**Use in Book**: Chapter 8 - External System Dependencies & Transaction Rollback

---

## 🎯 Which Diagram to Use?

| Learning Goal | Use This Diagram |
|---|---|
| **Intro to architecture** (high-level overview) | **Diagram 4** (`04-servlet-architecture.svg`) — shows all 5 flows side-by-side |
| **Deep dive into ONE failure type** (for each chapter) | **Diagram 5A-5E** (`05-servlet-architecture-0X-*.svg`) — individual, detailed, A4-optimized |
| **Show non-engineers** | Diagrams 1, 2, 3 (less technical) |
| **Troubleshooting guide** | Diagrams 4, 5A-5E (with log signals) |

---

## 🎯 Failure Injection Points (Marked in All Diagrams)

Each diagram clearly marks where failure injection occurs:

| Failure Type | Endpoint | Diagram(s) | Log Signature |
|---|---|---|---|
| **LDAP_TIMEOUT** | `/login` | 2 & 4 | "⚠️ LDAP Failure Injection: Simulating connection timeout" |
| **DB_LEAK** | `/api/menu` | 2 & 4 | "⚠️ Connection leak scenario" + "Cannot get a connection, pool error" |
| **OOM** | `/api/analytics?action=all-time-report` | 2 & 4 | "⚠️ OOM FAILURE INJECTION" + "java.lang.OutOfMemoryError: heap space" |
| **DEADLOCK** | `/api/kds`, `/checkout` | 2 & 4 | "❌ DEADLOCK SCENARIO: Both threads are now BLOCKED" + "ERROR: deadlock detected" |
| **SSL** | `/checkout` | 2 & 4 | "❌ SSL Certificate Validation Failed" |

---

## 📚 How to Use in Your Book

### Chapter 1: System Architecture
- **Reference**: Diagram 1 (Infrastructure Architecture)
- Explain the stack: JDK 21, Tomcat 11, PostgreSQL 15, OpenLDAP 1.5.0
- Show service-to-service communication via Docker Compose network
- Introduce logging pipeline and daily log rotation

### Chapter 2: Request Flow & Routing
- **Reference**: Diagram 2 (Application Flow)
- Walk through the HTTP filter chain (5 stages)
- Describe each endpoint's purpose
- Explain session creation and 30-minute timeout lifecycle

### Chapter 3: Database Model
- **Reference**: Diagram 3 (Schema Architecture)
- Show all 11 tables and relationships
- Explain transactional vs. analytical tables
- Highlight foreign key constraints and composite keys

### Chapter 1: System Architecture (Introduction)
- **Reference**: Diagram 4 (Servlet Architecture Overview)
- Show all 5 flows side-by-side for readers to understand the big picture
- Introduce the concept of failure injection
- Explain how to trigger each failure type

### Chapters 4-8: Failure Troubleshooting (One per Failure Type with Individual Diagrams)

#### Chapter 4: LDAP Authentication Timeout
- **Reference**: **Diagram 5A** (Use Case 1: LDAP Timeout - `05-servlet-architecture-01-login-ldap-timeout.svg`)
- Expanded detail: Shows 6-step flow from HTTP request through LDAP timeout
- Show LoginServlet → UserService → LDAP bind with failure point highlighted
- Diagnostic signals: Exact log patterns readers will see in catalina.log
- Troubleshooting checklist: How to verify LDAP server health

#### Chapter 5: Database Connection Pool Exhaustion
- **Reference**: **Diagram 5B** (Use Case 2: DB Leak - `05-servlet-architecture-02-menu-db-leak.svg`)
- Expanded detail: Shows HikariCP pool lifecycle and exhaustion timeline
- Show connection leak scenario (missing `close()` call)
- HikariCP timeline: From request 1-20 (slow) to request 21+ (immediate timeout)
- Diagnostic signals: "Cannot get a connection, pool error..."
- Monitoring tips: Count active connections, validate `close()` calls

#### Chapter 6: Out-Of-Memory Errors
- **Reference**: **Diagram 5C** (Use Case 3: OOM - `05-servlet-architecture-03-analytics-oom.svg`)
- Expanded detail: Shows heap usage timeline from idle to exhaustion
- Show unbounded query loading 10K events (100-150 bytes each)
- Heap timeline: 100MB idle → 200MB (1K events) → 350MB (5K) → 512MB (OOM)
- Diagnostic signals: "java.lang.OutOfMemoryError: Java heap space" + rapid GC cycles
- Solution tips: Add pagination, cursor-based fetching, memory limits

#### Chapter 7: Deadlock Detection
- **Reference**: **Diagram 5D** (Use Case 4: Deadlock - `05-servlet-architecture-04-kds-deadlock.svg`)
- Expanded detail: Shows concurrent transaction flows with circular lock dependency
- Lock dependency graph: Transaction 1 ↔ Transaction 2 (circular wait)
- Show how PostgreSQL detects deadlock and ROLLBACKs one transaction
- Diagnostic signals: "ERROR: deadlock detected" in PostgreSQL logs
- Prevention tips: Review lock ordering, use thread dumps to identify blocked threads

#### Chapter 8: SSL/TLS Certificate Failures
- **Reference**: **Diagram 5E** (Use Case 5: SSL - `05-servlet-architecture-05-checkout-ssl.svg`)
- Expanded detail: Shows transaction rollback sequence when SSL validation fails
- Transaction timeline: BEGIN → partial INSERTs (orders, order_items) → SSL failure → ROLLBACK
- Show why no rows are persisted despite partial writes
- Diagnostic signals: "SSLHandshakeException: PKIX validation failed" + ROLLBACK logs
- Troubleshooting guide: Certificate validation steps, self-signed cert handling in Tomcat

---

## 🔗 Diagram Cross-References

**Diagram 1** (Infrastructure) → **Diagram 2** (Request Flow)
- Shows how user requests enter Tomcat and flow through 5-stage filter chain

**Diagram 2** (Request Flow) → **Diagram 3** (Schema)
- Shows which tables each endpoint queries (menu_items, orders, analytics_events, etc.)

**Diagram 3** (Schema) → **Diagram 4** (Servlet Architecture)
- Details table structures and how DAOs execute prepared SQL

**Diagram 4** (Servlet Architecture) → **CLAUDE.md**
- References actual servlet files and failure injection code lines
- Links to expected catalina.log output signals

---

## 🎨 Visual Design Principles

All diagrams use:
- **Consistent color coding**:
  - Blue: Authentication & Users
  - Orange: Tomcat & Servlets
  - Purple: Roles & Permissions
  - Green: Transactions & Logging
  - Red: Failure Injection Points
- **Clear hierarchy**: Larger boxes for components, smaller for details
- **Flow arrows**: Show data and control flow direction
- **Annotations**: Brief, technical descriptions without code
- **Scalable SVG**: Renders perfectly at any zoom level for print/digital

---

## 📸 File Sizes & Performance

| Diagram | File Size | Complexity | Best Zoom | Purpose |
|---------|-----------|-----------|-----------|---------|
| 1 (Infrastructure) | ~15 KB | Low-Medium | 100%-150% | Architecture overview |
| 2 (Request Flow) | ~25 KB | Medium | 100%-125% | Flow tracing |
| 3 (Database Schema) | ~18 KB | Medium-High | 75%-100% | Schema reference |
| 4 (Servlet Architecture - Overview) | ~35 KB | High | 50%-100% | All 5 flows side-by-side |
| **5A** (LDAP Timeout) | ~22 KB | Medium | 100% | Chapter 4: Authentication failures |
| **5B** (DB Leak) | ~28 KB | Medium-High | 100% | Chapter 5: Resource leaks |
| **5C** (OOM) | ~30 KB | Medium-High | 100% | Chapter 6: Memory pressure |
| **5D** (Deadlock) | ~32 KB | High | 100% | Chapter 7: Lock contention |
| **5E** (SSL) | ~26 KB | Medium | 100% | Chapter 8: External dependencies |

**All diagrams optimized for:**
- **Print publication** (600+ dpi, A4 size, 100% zoom with readable fonts)
- **Digital display** (web, PDF, e-readers, screens)
- **Book chapter integration** (individual diagrams for deep dives, overview diagram for intro)

---

## 🔄 Integration with Documentation

- **README.md**: Overview + quick start (references Diagrams 1 & 2)
- **TESTING.md**: Test procedures (references Diagram 4 for failure injection)
- **DEPLOYMENT.md**: Production hardening (references Diagram 1 for scaling)
- **CLAUDE.md**: Development guidelines (references all diagrams & code lines)

---

## 📋 Update History

| Version | Date | Changes |
|---------|------|---------|
| 3.0 | June 2026 | ✅ **Added 5 individual use-case diagrams** (5A-5E) optimized for A4 print — one diagram per chapter for deep dives; kept Diagram 4 as overview reference |
| 2.0 | June 2026 | ✅ Regenerated all 4 SVG diagrams with improved visual design, added failure summary table, enhanced annotations |
| 1.0 | May 2026 | Initial diagram set with text-based ASCII art |

---

## ✅ Verification Checklist

- [x] All 4 original SVG diagrams (01-04) remain unchanged for reference
- [x] **5 new use-case diagrams created** (05A-05E) — one per failure type
- [x] Individual diagrams optimized for **A4 print** with readable fonts
- [x] Failure injection points clearly marked with numbered steps
- [x] Detailed diagnostic signals included in each diagram
- [x] Color coding consistent across all diagrams
- [x] Cross-references between diagrams documented
- [x] Chapter integration plan updated (one diagram per chapter)
- [x] File sizes optimized for web and print
- [x] All 9 diagrams tested for scalability (SVG renders at any zoom)
- [x] Documentation updated in DIAGRAMS_SUMMARY.md

---

**Last Updated**: June 2026  
**Status**: ✅ Production Ready for Book Publication  
**Maintainer**: CTO/Solution Architect  
**Format**: SVG (Scalable Vector Graphics) — Platform Independent
