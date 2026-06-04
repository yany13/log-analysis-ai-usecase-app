# ByteBite Book Diagrams — Visual Reference Guide (Updated)

## Overview

Four professional, publication-ready architecture diagrams have been created for the ByteBite troubleshooting guide. These diagrams illustrate:
- **Infrastructure topology** (user to database flow)
- **Application request flows** (login, menu, analytics, checkout, KDS)
- **Database schema architecture** (11 tables and relationships)
- **Servlet-to-database connection patterns** (HikariCP, failure injection points)

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

### 4. **Servlet & Database Connection Architecture** (`04-servlet-architecture.svg`)
**File**: `docs/04-servlet-architecture.svg`

**Shows**:
- **5 detailed flow diagrams** showing servlet → service → DAO → HikariCP → SQL:

  1. **LOGIN FLOW** (with LDAP_TIMEOUT injection at line 141)
     - UserService → LDAP bind attempt
     - UserDAO → PreparedStatement SELECT users
     - HikariCP connection pool lifecycle
  
  2. **MENU FLOW** (with DB_LEAK injection at line 79)
     - MenuService → MenuDAO
     - Connection leak scenario: missing `conn.close()`
     - Pool exhaustion progression after N requests
  
  3. **ANALYTICS FLOW** (with OOM injection at line 96)
     - AnalyticsService → AnalyticsDAO
     - Loading all 10K events into memory (100KB each = 1GB heap)
     - OutOfMemoryError with Xmx=512m
  
  4. **CHECKOUT FLOW** (with DEADLOCK & SSL injection at lines 68+)
     - CheckoutServlet → OrderService → OrderDAO
     - Transaction with 3 inserts (orders, order_items, kds_orders)
     - Lock ordering: orders 1st → inventory 2nd (normal path)
  
  5. **KDS FLOW** (with DEADLOCK injection at line 55)
     - KDSServlet → KDSService → KDSOrderDAO
     - Reverse lock ordering: inventory 1st → orders 2nd (deadlock path)
     - Circular wait deadlock visualization

- **HikariCP pool state**: idle, active, waiting, exhausted
- **PreparedStatement execution**: SQL injection prevention
- **Connection lifecycle**: getConnection() → execute → close() → back to pool
- **Failure Summary Table**: All 5 failure types with triggers, root causes, log signals

**Key Talking Points**:
- PreparedStatements: All SQL uses parameterized queries (SQL injection protection)
- Connection lifecycle: getConnection() → execute → close() → back to pool
- Failure injection points: Marked at specific code locations where failures activate
- Thread safety: HikariCP is thread-safe; deadlock occurs at DB level

**Use in Book**: Chapters 4-8 - Troubleshooting each failure type & connection pooling deep dives

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

### Chapters 4-8: Failure Troubleshooting (One per Failure Type)

#### Chapter 4: LDAP Authentication Timeout
- **Reference**: Diagram 4 (Servlet Architecture - LOGIN FLOW)
- Show LoginServlet → UserService → LDAP bind
- Explain "LDAP timeout" log signature
- Guide readers to check LDAP server health in logs

#### Chapter 5: Database Connection Pool Exhaustion
- **Reference**: Diagram 4 (Servlet Architecture - MENU FLOW)
- Show HikariCP lifecycle and pool states
- Explain connection leak (missing `close()`)
- Show pool exhaustion progression from 5 idle → exhausted

#### Chapter 6: Out-Of-Memory Errors
- **Reference**: Diagram 4 (Servlet Architecture - ANALYTICS FLOW)
- Show unbounded query loading 10K events × 100KB each
- Explain heap exhaustion with Xmx=512m
- Link to log signatures in catalina.log

#### Chapter 7: Deadlock Detection
- **Reference**: Diagram 4 (Servlet Architecture - CHECKOUT/KDS FLOW)
- Show two threads with reverse lock ordering
- Explain circular wait scenario (orders ↔ kds_orders)
- Guide readers to PostgreSQL deadlock detection in logs

#### Chapter 8: SSL/TLS Certificate Failures
- **Reference**: Diagram 4 (Servlet Architecture - SSL section)
- Show where certificate validation occurs
- Explain self-signed cert handling in Tomcat
- Guide readers to TLS exceptions in catalina.log

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

| Diagram | File Size | Complexity | Best Zoom |
|---------|-----------|-----------|-----------|
| 1 (Infrastructure) | ~15 KB | Low-Medium | 100%-150% |
| 2 (Request Flow) | ~25 KB | Medium | 100%-125% |
| 3 (Database Schema) | ~18 KB | Medium-High | 75%-100% |
| 4 (Servlet Architecture) | ~35 KB | High | 50%-100% |

All diagrams optimized for both **print publication** (600+ dpi) and **digital display** (web, PDF, e-readers).

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
| 2.0 | June 2026 | ✅ Regenerated all 4 SVG diagrams with improved visual design, added failure summary table, enhanced annotations |
| 1.0 | May 2026 | Initial diagram set with text-based ASCII art |

---

## ✅ Verification Checklist

- [x] All 4 SVG diagrams regenerated and optimized
- [x] Failure injection points clearly marked in all diagrams
- [x] Color coding consistent across all diagrams
- [x] Cross-references between diagrams documented
- [x] Pre-seeded data examples included
- [x] Log signal patterns documented
- [x] Chapter integration plan provided
- [x] File sizes optimized for web and print
- [x] All diagrams tested for scalability

---

**Last Updated**: June 2026  
**Status**: ✅ Production Ready for Book Publication  
**Maintainer**: CTO/Solution Architect  
**Format**: SVG (Scalable Vector Graphics) — Platform Independent
