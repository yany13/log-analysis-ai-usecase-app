# ByteBite Architecture & Flow Diagrams

## 1. Infrastructure Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              BYTEBITE INFRASTRUCTURE                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ┌──────────────────┐                                                        │
│  │   BROWSER USER   │                                                        │
│  │  (HTTP Client)   │                                                        │
│  └────────┬─────────┘                                                        │
│           │ HTTP/HTTPS                                                       │
│           │ (Port 8080/8443)                                                 │
│           ▼                                                                   │
│  ┌──────────────────────────────────────────────────────────────────┐       │
│  │                    APACHE TOMCAT 11                             │       │
│  │                  (JDK Temurin 21)                               │       │
│  │  ┌──────────────────────────────────────────────────────────┐  │       │
│  │  │             6 Servlets + 4 Filters                      │  │       │
│  │  │  • LoginServlet        • RequestLoggingFilter           │  │       │
│  │  │  • MenuServlet         • LDAPAuthenticationFilter       │  │       │
│  │  │  • CheckoutServlet     • CORSFilter                     │  │       │
│  │  │  • AnalyticsServlet    • SecurityHeadersFilter          │  │       │
│  │  │  • KDSServlet          • FailureInjectionUtil           │  │       │
│  │  │  • HealthServlet                                        │  │       │
│  │  └──────────────────────────────────────────────────────────┘  │       │
│  └────┬──────────────────────┬──────────────────────────────┬────┘       │
│       │                      │                              │               │
│       │ LDAP Bind           │ JDBC Connection             │ Logs         │
│       │ (Port 389)          │ (Port 5432)                 │ (Daily)      │
│       ▼                      ▼                              ▼               │
│  ┌─────────────────┐  ┌──────────────────┐  ┌──────────────────────┐    │
│  │  OPENLDAP 1.5.0 │  │  POSTGRESQL 15   │  │  Catalina.log        │    │
│  │                 │  │   -alpine        │  │  (Daily rotation)    │    │
│  │ DC=bytebite,    │  │                  │  │  catalina.YYYY-MM-DD │    │
│  │ DC=local        │  │ • Users          │  │  .log                │    │
│  │                 │  │ • Sessions       │  │                      │    │
│  │ 6 Test Users:   │  │ • Orders         │  │ SLF4J Appender:      │    │
│  │ • admin         │  │ • Menu Items     │  │ AsyncFileHandler     │    │
│  │ • manager       │  │ • Analytics      │  │ (JUL)                │    │
│  │ • staff1/2      │  │ • KDS Orders     │  │                      │    │
│  │ • kds_operator  │  │ • API Audit Log  │  │ Rotation: Daily      │    │
│  │ • customer1     │  │                  │  │ Retention: 90 days   │    │
│  │                 │  │ HikariCP:        │  │                      │    │
│  │                 │  │ Max 20 Conn.     │  │                      │    │
│  └─────────────────┘  └──────────────────┘  └──────────────────────┘    │
│                                                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │                      DOCKER COMPOSE NETWORK                         │  │
│  │                  (Bridge: bytebite-network)                         │  │
│  │   All services communicate via internal DNS: <container_name>       │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
│                                                                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Key Infrastructure Points:**
- **User Entry**: HTTP/HTTPS on ports 8080 (HTTP) and 8443 (HTTPS)
- **Authentication**: LDAP bind to OpenLDAP (389)
- **Data Persistence**: PostgreSQL HikariCP connection pool (max 20 connections)
- **Logging**: Asynchronous JUL handler → `catalina.YYYY-MM-DD.log` (daily rotation, 90-day retention)
- **Network**: Internal Docker bridge network for service-to-service communication

---

## 2. Application Code Logic & Session Flow Diagram

```
┌──────────────────────────────────────────────────────────────────────────┐
│           BYTEBITE APPLICATION REQUEST/SESSION FLOW                      │
│                                                                            │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ HTTP Request arrives at Tomcat                                  │    │
│  └──────────────────────┬──────────────────────────────────────────┘    │
│                         │                                                 │
│          ┌──────────────▼──────────────┐                                 │
│          │  RequestLoggingFilter       │                                 │
│          │ [Logs all requests]         │                                 │
│          └──────────────┬──────────────┘                                 │
│                         │                                                 │
│          ┌──────────────▼──────────────────────────┐                    │
│          │  LDAPAuthenticationFilter               │                    │
│          │  [Check session/auth, bypass if        │                    │
│          │   ?failureType param present]          │                    │
│          └──────────────┬──────────────────────────┘                    │
│                         │                                                 │
│          ┌──────────────▼──────────────┐                                 │
│          │  CORSFilter + Headers       │                                 │
│          │  [Security headers]         │                                 │
│          └──────────────┬──────────────┘                                 │
│                         │                                                 │
│          ┌──────────────▼──────────────────────────────────┐             │
│          │         SERVLET DISPATCHER                      │             │
│          │  (Route to appropriate servlet)                │             │
│          └──┬──────────────┬──────────────┬──────────────┬┘             │
│             │              │              │              │               │
│      ┌──────▼─┐    ┌──────▼──┐   ┌──────▼──┐   ┌──────▼──┐             │
│      │ Login  │    │ Menu    │   │Analytics│   │KDS/     │             │
│      │Servlet │    │Servlet  │   │Servlet  │   │Checkout │             │
│      └────────┘    └─────────┘   └─────────┘   └─────────┘             │
│                                                                            │
└──────────────────────────────────────────────────────────────────────────┘

╔══════════════════════════════════════════════════════════════════════════╗
║              DETAILED FLOW: LOGIN → MENU → CHECKOUT                      ║
╚══════════════════════════════════════════════════════════════════════════╝

╔═══════════════════════════════════════════════════════════════════════════╗
║ 1. LOGIN FLOW  [POST /bytebite/login]                                     ║
╠═══════════════════════════════════════════════════════════════════════════╣
║                                                                             ║
║  User Input:                     Backend Processing:                       ║
║  ┌───────────────┐              ┌──────────────────────────────────────┐  ║
║  │ Username      │──────────────▶│ 1. Validate credentials against     │  ║
║  │ Password      │              │    LDAP (OpenLDAP)                 │  ║
║  └───────────────┘              │                                      │  ║
║                                  │ 2. If LDAP auth succeeds:           │  ║
║  Database Ops:                   │    - Query users table              │  ║
║  ┌───────────────────────────┐  │    - Update last_login             │  ║
║  │ SELECT * FROM users       │  │    - Insert into sessions table    │  ║
║  │ WHERE username = ?        │  │                                      │  ║
║  │                            │  │ 3. Generate session_id              │  ║
║  │ UPDATE users              │  │    Set expires_at = now + 30min    │  ║
║  │ SET last_login = now()    │  │                                      │  ║
║  │                            │  │ 4. Return session cookie            │  ║
║  │ INSERT INTO sessions      │  │                                      │  ║
║  │ (session_id, user_id,     │  │ 5. Log: "User admin logged in"      │  ║
║  │  expires_at, ...)         │  │                                      │  ║
║  └───────────────────────────┘  └──────────────────────────────────────┘  ║
║                                                                             ║
║  Response: Session Cookie → Browser                                       ║
║           Set-Cookie: JSESSIONID=xyz123...                               ║
║                                                                             ║
╚═══════════════════════════════════════════════════════════════════════════╝

╔═══════════════════════════════════════════════════════════════════════════╗
║ 2. MENU FLOW  [GET /bytebite/menu/ or /api/menu]                          ║
╠═══════════════════════════════════════════════════════════════════════════╣
║                                                                             ║
║  Request Header:                 Backend Processing:                       ║
║  ┌──────────────────────────┐   ┌──────────────────────────────────────┐  ║
║  │ Cookie: JSESSIONID=xyz   │──▶│ 1. Validate session from cookie      │  ║
║  └──────────────────────────┘   │    Query sessions table               │  ║
║                                  │                                       │  ║
║  Database Ops:                   │ 2. Check session active & not expired│  ║
║  ┌──────────────────────────┐   │                                       │  ║
║  │ SELECT * FROM sessions   │   │ 3. Execute menu query:               │  ║
║  │ WHERE session_id = ?     │   │    SELECT * FROM menu_items          │  ║
║  │ AND is_active = true     │   │    WHERE available = true            │  ║
║  │ AND expires_at > now()   │   │                                       │  ║
║  │                           │   │ 4. Render menu (16 items):           │  ║
║  │ SELECT * FROM            │   │    • 3 Pizzas                        │  ║
║  │   menu_items             │   │    • 3 Burgers                       │  ║
║  │ WHERE available = true   │   │    • 3 Pastas                        │  ║
║  │ ORDER BY category        │   │    • 2 Salads                        │  ║
║  │                           │   │    • 3 Desserts                      │  ║
║  │ [DB_LEAK INJECTION HERE] │   │    • 2 Beverages                     │  ║
║  └──────────────────────────┘   │                                       │  ║
║                                  │ 5. Log event: page_view              │  ║
║                                  │    INSERT INTO analytics_events      │  ║
║                                  │    (event_type, user_id, session_id) │  ║
║                                  └──────────────────────────────────────┘  ║
║                                                                             ║
║  Response: HTML Menu Page with Items & Prices                             ║
║                                                                             ║
╚═══════════════════════════════════════════════════════════════════════════╝

╔═══════════════════════════════════════════════════════════════════════════╗
║ 3. ANALYTICS FLOW  [GET /bytebite/analytics/ + ?action=all-time-report]   ║
╠═══════════════════════════════════════════════════════════════════════════╣
║                                                                             ║
║  Request Header:                 Backend Processing:                       ║
║  ┌──────────────────────────┐   ┌──────────────────────────────────────┐  ║
║  │ Cookie: JSESSIONID=xyz   │──▶│ 1. Validate session                  │  ║
║  │ ?action=all-time-report  │   │                                       │  ║
║  └──────────────────────────┘   │ 2. Check role: ADMIN/MANAGER only    │  ║
║                                  │                                       │  ║
║  Database Ops:                   │ 3. Query all analytics events:       │  ║
║  ┌──────────────────────────┐   │    SELECT * FROM analytics_events    │  ║
║  │ SELECT * FROM            │   │    ORDER BY created_at DESC          │  ║
║  │   analytics_events       │   │    [~10,000 events = OOM trigger]    │  ║
║  │ ORDER BY created_at DESC │   │                                       │  ║
║  │ [OOM INJECTION HERE]     │   │ 4. Load all 10K events into memory   │  ║
║  │                           │   │    Parse JSON event_data            │  ║
║  │ ~10,000 rows             │   │    Aggregate by type                 │  ║
║  │ each 100KB JSONB field   │   │                                       │  ║
║  └──────────────────────────┘   │ 5. Generate report:                  │  ║
║                                  │    PAGE_VIEW: XXXX                   │  ║
║                                  │    PURCHASE: YYYY                    │  ║
║                                  │    CHECKOUT: ZZZZ                    │  ║
║                                  │                                       │  ║
║                                  │ 6. Log: "Report generated"           │  ║
║                                  └──────────────────────────────────────┘  ║
║                                                                             ║
║  Response: HTML Analytics Report Page                                      ║
║  (or OutOfMemoryError if heap exhausted)                                   ║
║                                                                             ║
╚═══════════════════════════════════════════════════════════════════════════╝

╔═══════════════════════════════════════════════════════════════════════════╗
║ 4. CHECKOUT FLOW  [POST /bytebite/checkout/]                              ║
╠═══════════════════════════════════════════════════════════════════════════╣
║                                                                             ║
║  Request Header + Body:          Backend Processing:                       ║
║  ┌──────────────────────────┐   ┌──────────────────────────────────────┐  ║
║  │ Cookie: JSESSIONID=xyz   │──▶│ 1. Validate session (CUSTOMER role)  │  ║
║  │ Content-Type: form-data  │   │                                       │  ║
║  │                           │   │ 2. Validate cart items               │  ║
║  │ Item IDs & Qty:          │   │    SELECT * FROM menu_items          │  ║
║  │ • item_1 qty 2           │   │    WHERE id IN (item_1, item_2...)   │  ║
║  │ • item_3 qty 1           │   │                                       │  ║
║  │                           │   │ 3. Calculate total & tax             │  ║
║  │ Shipping/Billing Address │   │    total = sum(qty * price)          │  ║
║  │                           │   │    tax = total * 0.08               │  ║
║  │ [SSL_FAILURE INJECTION]  │   │    shipping = 5.00                   │  ║
║  └──────────────────────────┘   │                                       │  ║
║                                  │ 4. BEGIN TRANSACTION                 │  ║
║  Database Ops:                   │                                       │  ║
║  ┌──────────────────────────┐   │ 5. INSERT INTO orders                │  ║
║  │ BEGIN TRANSACTION        │   │    (user_id, order_number,          │  ║
║  │                           │   │     total_amount, tax, shipping)    │  ║
║  │ INSERT INTO orders       │   │                                       │  ║
║  │ (user_id, order_number,  │   │ 6. INSERT INTO order_items           │  ║
║  │  total_amount, tax,      │   │    (order_id, product_id, qty,      │  ║
║  │  shipping_address, ...)  │   │     unit_price, total_price)        │  ║
║  │                           │   │                                       │  ║
║  │ INSERT INTO order_items  │   │ 7. INSERT INTO kds_orders            │  ║
║  │ (order_id, product_id,   │   │    (order_id, kds_status=NEW)       │  ║
║  │  quantity, ...)          │   │                                       │  ║
║  │                           │   │ 8. [SSL Certificate check here]      │  ║
║  │ INSERT INTO kds_orders   │   │    [SSL_FAILURE INJECTION]           │  ║
║  │ (order_id, kds_status)   │   │                                       │  ║
║  │                           │   │ 9. COMMIT TRANSACTION (or ROLLBACK)  │  ║
║  │ [DEADLOCK INJECTION HERE]│   │                                       │  ║
║  │ (Lock order 1st,         │   │ 10. Notify KDS:                      │  ║
║  │  then inventory)         │   │     Send order to Kitchen Display    │  ║
║  │                           │   │     System                           │  ║
║  │ INSERT INTO analytics    │   │                                       │  ║
║  │ _events (PURCHASE)       │   │ 11. Log event: purchase_complete    │  ║
║  │                           │   │                                       │  ║
║  │ COMMIT                   │   │ 12. Return: Order Confirmation       │  ║
║  └──────────────────────────┘   │     Order ID, Total, Receipt         │  ║
║                                  └──────────────────────────────────────┘  ║
║                                                                             ║
║  Response: Order Confirmation Page (or error if SSL/deadlock/OOM)          ║
║                                                                             ║
╚═══════════════════════════════════════════════════════════════════════════╝

╔═══════════════════════════════════════════════════════════════════════════╗
║ 5. KDS (KITCHEN DISPLAY SYSTEM) FLOW  [POST /bytebite/api/kds]            ║
╠═══════════════════════════════════════════════════════════════════════════╣
║                                                                             ║
║  Request Header + JSON:          Backend Processing:                       ║
║  ┌──────────────────────────┐   ┌──────────────────────────────────────┐  ║
║  │ Cookie: JSESSIONID=xyz   │──▶│ 1. Validate session (KDS_OPERATOR)   │  ║
║  │ Content-Type: app/json   │   │                                       │  ║
║  │                           │   │ 2. Parse JSON:                       │  ║
║  │ {                         │   │    {orderId: "ORD-123",              │  ║
║  │   "orderId": "ORD-123",   │   │     status: "IN_PROGRESS"}           │  ║
║  │   "status": "IN_PROGRESS" │   │                                       │  ║
║  │ }                         │   │ 3. UPDATE kds_orders                 │  ║
║  │                           │   │    WHERE order_id = ? AND            │  ║
║  │ [DEADLOCK INJECTION HERE] │   │    WHERE inventory_id = ?            │  ║
║  │ (Lock inventory 1st,      │   │    (Lock order after lock inventory) │  ║
║  │  then order)              │   │    [DEADLOCK if checkout locks       │  ║
║  │                           │   │     order 1st]                       │  ║
║  └──────────────────────────┘   │                                       │  ║
║                                  │ 4. UPDATE orders status              │  ║
║  Database Ops:                   │                                       │  ║
║  ┌──────────────────────────┐   │ 5. Log: "KDS status updated"         │  ║
║  │ BEGIN TRANSACTION        │   │                                       │  ║
║  │                           │   │ 6. Return: {"status": "updated"}     │  ║
║  │ UPDATE kds_orders        │   │                                       │  ║
║  │ SET kds_status = ?       │   │ (or ThreadDump if deadlock occurs)    │  ║
║  │ WHERE order_id = ?       │   │                                       │  ║
║  │                           │   │                                       │  ║
║  │ UPDATE orders            │   │                                       │  ║
║  │ SET status = ?           │   │                                       │  ║
║  │ WHERE order_id = ?       │   │                                       │  ║
║  │ [DEADLOCK scenario:      │   │                                       │  ║
║  │  Thread 1 waits on       │   │                                       │  ║
║  │  inventory lock]         │   │                                       │  ║
║  │                           │   │                                       │  ║
║  │ COMMIT                   │   │                                       │  ║
║  └──────────────────────────┘   │                                       │  ║
║                                  └──────────────────────────────────────┘  ║
║                                                                             ║
║  Response: JSON Status or ThreadDump (if deadlock)                         ║
║                                                                             ║
╚═══════════════════════════════════════════════════════════════════════════╝
```

---

## 3. Database Table Structure Diagram

```
╔════════════════════════════════════════════════════════════════════════╗
║                  BYTEBITE DATABASE SCHEMA (PostgreSQL 15)             ║
║                      SCHEMA: bytebite                                 ║
╚════════════════════════════════════════════════════════════════════════╝

┌─────────────────────────────────────────────────────────────────────────┐
│ TABLE: users (PK: user_id SERIAL)                                       │
├─────────────────────────────────────────────────────────────────────────┤
│  user_id (INT)           │ Primary Key, Auto-increment                 │
│  username (VARCHAR 255)  │ Unique, NOT NULL — linked to LDAP DN       │
│  email (VARCHAR 255)     │ Unique, nullable                            │
│  ldap_dn (VARCHAR 512)   │ OpenLDAP Distinguished Name                 │
│  password_hash (VARCHAR) │ Never used; LDAP auth takes precedence      │
│  is_active (BOOLEAN)     │ Account activation flag                     │
│  is_admin (BOOLEAN)      │ Admin role flag (also in user_roles)        │
│  created_at (TIMESTAMP)  │ Account creation timestamp                  │
│  updated_at (TIMESTAMP)  │ Last profile update                         │
│  last_login (TIMESTAMP)  │ Updated on successful login                 │
├─────────────────────────────────────────────────────────────────────────┤
│ Indices: idx_users_username, idx_users_email                            │
│ Relationships: ◄── user_roles (M:M), ◄── sessions, ◄── orders          │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ TABLE: sessions (PK: session_id VARCHAR 128)                            │
├─────────────────────────────────────────────────────────────────────────┤
│  session_id (VARCHAR 128) │ Primary Key, JSESSIONID                    │
│  user_id (INT)            │ FK → users.user_id (CASCADE)               │
│  session_data (TEXT)      │ Serialized session attributes              │
│  ip_address (VARCHAR 45)  │ IPv4/IPv6 client IP                        │
│  user_agent (TEXT)        │ Browser info                               │
│  created_at (TIMESTAMP)   │ Session creation time                      │
│  last_accessed (TIMESTAMP)│ Last activity timestamp                    │
│  expires_at (TIMESTAMP)   │ Expiration time (typically +30 min)        │
│  is_active (BOOLEAN)      │ Session validity flag                      │
├─────────────────────────────────────────────────────────────────────────┤
│ Indices: idx_sessions_user_id, idx_sessions_expires_at                  │
│ Relationships: ► users (N:1), ◄── analytics_events                      │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ TABLE: menu_items (PK: id SERIAL)                                       │
├─────────────────────────────────────────────────────────────────────────┤
│  id (SERIAL)              │ Primary Key, Auto-increment                │
│  name (VARCHAR 255)       │ Menu item name — NOT NULL                 │
│  price (DECIMAL 10,2)     │ Unit price — NOT NULL                     │
│  description (TEXT)       │ Item description                          │
│  category (VARCHAR 100)   │ Category: Pizza, Burgers, Pasta, etc.    │
│  available (BOOLEAN)      │ Availability flag (default true)          │
│  created_at (TIMESTAMP)   │ Item added timestamp                      │
│  updated_at (TIMESTAMP)   │ Last modification timestamp               │
├─────────────────────────────────────────────────────────────────────────┤
│ Indices: idx_menu_items_category, idx_menu_items_available              │
│ Sample Data: 16 items seeded (3 pizzas, 3 burgers, 3 pastas, etc.)     │
│ Relationships: ◄── order_items (1:M)                                    │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ TABLE: orders (PK: order_id BIGSERIAL)                                  │
├─────────────────────────────────────────────────────────────────────────┤
│  order_id (BIGSERIAL)     │ Primary Key, Auto-increment                │
│  user_id (INT)            │ FK → users.user_id (RESTRICT)             │
│  order_number (VARCHAR 50)│ Unique order reference                    │
│  status (VARCHAR 50)      │ PENDING, IN_PROGRESS, READY, COMPLETED   │
│  total_amount (DECIMAL)   │ Order total (without tax/shipping)        │
│  tax_amount (DECIMAL)     │ Calculated tax (8% default)               │
│  shipping_amount (DECIMAL)│ Shipping cost                             │
│  shipping_address (TEXT)  │ Delivery address                          │
│  billing_address (TEXT)   │ Billing address                           │
│  order_date (TIMESTAMP)   │ Order creation timestamp                  │
│  last_updated (TIMESTAMP) │ Last status update timestamp              │
│  notes (TEXT)             │ Special instructions                      │
├─────────────────────────────────────────────────────────────────────────┤
│ Indices: idx_orders_user_id, idx_orders_status, idx_orders_order_date   │
│ Relationships: ► users (N:1), ◄── order_items, ◄── kds_orders          │
│ Failure Injection: DEADLOCK scenario (lock contention with KDS)         │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ TABLE: order_items (PK: order_item_id BIGSERIAL)                        │
├─────────────────────────────────────────────────────────────────────────┤
│  order_item_id (BIGSERIAL)│ Primary Key, Auto-increment               │
│  order_id (BIGINT)        │ FK → orders.order_id (CASCADE)            │
│  product_id (VARCHAR 100) │ Menu item reference                       │
│  product_name (VARCHAR)   │ Item name at purchase time                │
│  quantity (INT)           │ Number of items                           │
│  unit_price (DECIMAL)     │ Price per unit                            │
│  total_price (DECIMAL)    │ quantity × unit_price                     │
│  created_at (TIMESTAMP)   │ Item addition timestamp                   │
├─────────────────────────────────────────────────────────────────────────┤
│ Indices: idx_order_items_order_id                                       │
│ Relationships: ► orders (N:1), ► menu_items                             │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ TABLE: kds_orders (PK: kds_order_id BIGSERIAL)                          │
├─────────────────────────────────────────────────────────────────────────┤
│  kds_order_id (BIGSERIAL) │ Primary Key, Auto-increment               │
│  order_id (BIGINT)        │ FK → orders.order_id (CASCADE)            │
│  kds_status (VARCHAR 50)  │ NEW, IN_PROGRESS, READY, COMPLETED       │
│  estimated_ready_time     │ Expected completion (kitchen estimate)    │
│  actual_ready_time        │ Actual completion timestamp               │
│  priority_level (INT)     │ 0=normal, >0 = expedited                  │
│  notes (TEXT)             │ Preparation instructions                  │
│  created_at (TIMESTAMP)   │ Order sent to kitchen timestamp           │
│  updated_at (TIMESTAMP)   │ Last status change timestamp              │
├─────────────────────────────────────────────────────────────────────────┤
│ Indices: idx_kds_orders_status, idx_kds_orders_priority                 │
│ Relationships: ► orders (N:1)                                            │
│ Failure Injection: DEADLOCK scenario (opposite lock order from orders) │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ TABLE: analytics_events (PK: event_id BIGSERIAL)                        │
├─────────────────────────────────────────────────────────────────────────┤
│  event_id (BIGSERIAL)     │ Primary Key, Auto-increment               │
│  event_type (VARCHAR 100) │ PAGE_VIEW, PURCHASE, CHECKOUT, etc.      │
│  event_data (JSONB)       │ Flexible event attributes (100KB avg)     │
│  user_id (INT)            │ FK → users.user_id (SET NULL on delete)   │
│  session_id (VARCHAR 128) │ FK → sessions.session_id (SET NULL)       │
│  page_url (VARCHAR 512)   │ URL visited                               │
│  referrer_url (VARCHAR)   │ Previous page                             │
│  ip_address (VARCHAR 45)  │ Client IP                                 │
│  user_agent (TEXT)        │ Browser info                              │
│  created_at (TIMESTAMP)   │ Event timestamp                           │
├─────────────────────────────────────────────────────────────────────────┤
│ Indices: idx_analytics_event_type, idx_analytics_created_at,            │
│          idx_analytics_user_id                                          │
│ Sample Data: ~10,000 rows seeded for testing (OOM failure injection)    │
│ Relationships: ► users, ► sessions                                       │
│ Failure Injection: OOM scenario (unbounded query on all-time-report)    │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ TABLE: user_roles (PK: role_id SERIAL)                                  │
├─────────────────────────────────────────────────────────────────────────┤
│  role_id (SERIAL)         │ Primary Key, Auto-increment               │
│  role_name (VARCHAR 100)  │ ADMIN, MANAGER, STAFF, CUSTOMER, etc.    │
│  description (TEXT)       │ Role purpose                              │
│  created_at (TIMESTAMP)   │ Creation timestamp                        │
├─────────────────────────────────────────────────────────────────────────┤
│ Seeded Roles: ADMIN, MANAGER, STAFF, CUSTOMER, KDS_OPERATOR            │
│ Relationships: ◄── user_role_mapping (1:M)                              │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ TABLE: user_role_mapping (PK: user_id, role_id)                         │
├─────────────────────────────────────────────────────────────────────────┤
│  user_id (INT)            │ FK → users.user_id (CASCADE)             │
│  role_id (INT)            │ FK → user_roles.role_id (CASCADE)        │
│  assigned_at (TIMESTAMP)  │ Role assignment timestamp                │
├─────────────────────────────────────────────────────────────────────────┤
│ Composite Primary Key: (user_id, role_id)                               │
│ Relationships: ► users, ► user_roles (M:M junction table)               │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ TABLE: application_logs (PK: log_id BIGSERIAL)                          │
├─────────────────────────────────────────────────────────────────────────┤
│  log_id (BIGSERIAL)       │ Primary Key, Auto-increment               │
│  level (VARCHAR 20)       │ INFO, WARN, ERROR, DEBUG                  │
│  category (VARCHAR 255)   │ Logger category (e.g., com.bytebite.*)    │
│  message (TEXT)           │ Log message                               │
│  stack_trace (TEXT)       │ Exception stack trace (if error)          │
│  source_class (VARCHAR)   │ Calling class                             │
│  source_method (VARCHAR)  │ Calling method                            │
│  line_number (INT)        │ Source line number                        │
│  thread_name (VARCHAR)    │ Thread name                               │
│  user_id (INT)            │ FK → users.user_id (SET NULL)            │
│  session_id (VARCHAR 128) │ FK → sessions.session_id (SET NULL)       │
│  created_at (TIMESTAMP)   │ Log timestamp                             │
│  request_id (VARCHAR 128) │ Correlation ID for request tracing        │
├─────────────────────────────────────────────────────────────────────────┤
│ Indices: idx_logs_level, idx_logs_category, idx_logs_created_at,        │
│          idx_logs_user_id                                               │
│ Purpose: Audit trail for debugging and compliance                       │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ TABLE: api_audit_log (PK: audit_id BIGSERIAL)                           │
├─────────────────────────────────────────────────────────────────────────┤
│  audit_id (BIGSERIAL)     │ Primary Key, Auto-increment               │
│  user_id (INT)            │ FK → users.user_id (SET NULL)            │
│  api_endpoint (VARCHAR)   │ URL path (e.g., /bytebite/api/menu)      │
│  http_method (VARCHAR 10) │ GET, POST, PUT, DELETE                   │
│  request_body (TEXT)      │ Request payload (if POST/PUT)             │
│  response_status (INT)    │ HTTP status code (200, 500, etc.)         │
│  response_time_ms (INT)   │ Request duration in milliseconds          │
│  ip_address (VARCHAR 45)  │ Client IP                                 │
│  created_at (TIMESTAMP)   │ Request timestamp                         │
├─────────────────────────────────────────────────────────────────────────┤
│ Indices: idx_audit_log_user_id, idx_audit_log_created_at                │
│ Purpose: API usage audit and performance tracking                       │
└─────────────────────────────────────────────────────────────────────────┘

╔════════════════════════════════════════════════════════════════════════╗
║                      RELATIONSHIP DIAGRAM                             ║
╚════════════════════════════════════════════════════════════════════════╝

                           user_roles (1:M)
                                 ▲
                                 │
                    user_role_mapping (Junction)
                           │           │
                           ▼           ▼
                        users ◄────────┘
                     (PK: user_id)
                           │
                 ┌─────────┼──────────┬─────────┐
                 │         │          │         │
                 ▼         ▼          ▼         ▼
            sessions   orders   analytics_events  application_logs
        (user_id, FK) (user_id) (user_id, FK)    (user_id, FK)
             │            │           │
             │            ├──────┐    │
             │            ▼      │    │
             └────── order_items │    │
                                 │    │
                                 │    │
                        kds_orders    │
                        (order_id)    │
                                 │    │
                                 └────┘
                             menu_items
                           (product_id)
```

---

## 4. Servlet & Database Connection Architecture Diagram

```
╔════════════════════════════════════════════════════════════════════════╗
║         SERVLET → SERVICE → DAO → DATABASE CONNECTION FLOW            ║
║                    (High-Level Architecture)                           ║
╚════════════════════════════════════════════════════════════════════════╝

┌─────────────────────────────────────────────────────────────────────────┐
│                      HTTP REQUEST ARRIVES                              │
├─────────────────────────────────────────────────────────────────────────┤
│  ▼                                                                       │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ 1. RequestLoggingFilter                                        │   │
│  │    └──▶ Logs request metadata (method, path, headers)          │   │
│  │         Writes to catalina.log via SLF4J                       │   │
│  └──────────────────────────┬──────────────────────────────────────┘   │
│                             ▼                                           │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ 2. LDAPAuthenticationFilter                                    │   │
│  │    ├──▶ Check if session cookie present                        │   │
│  │    ├──▶ Query sessions table (HikariCP connection)             │   │
│  │    ├──▶ Validate session expiration & is_active = true         │   │
│  │    ├──▶ If ?failureType param: bypass auth (for testing)       │   │
│  │    └──▶ Store UserPrincipal in request context                 │   │
│  └──────────────────────────┬──────────────────────────────────────┘   │
│                             ▼                                           │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ 3. CORSFilter + SecurityHeadersFilter                          │   │
│  │    └──▶ Add security headers (X-Frame-Options, CSP, etc.)      │   │
│  │         Add CORS headers if applicable                         │   │
│  └──────────────────────────┬──────────────────────────────────────┘   │
│                             ▼                                           │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ 4. SERVLET ROUTING (Based on URL path)                         │   │
│  └──────────────────────────┬──────────────────────────────────────┘   │
│                             ▼                                           │
└─────────────────────────────────────────────────────────────────────────┘

╔═════════════════════════════════════════════════════════════════════════╗
║  SERVLET: LoginServlet → UserService → SessionDAO                      ║
╠═════════════════════════════════════════════════════════════════════════╣
║                                                                          ║
║  POST /bytebite/login                                                   ║
║                                                                          ║
║  ┌─────────────────────────────────────────────────────────────────┐   ║
║  │ LoginServlet.doPost(req, resp)                                  │   ║
║  │  1. Extract username, password from form                        │   ║
║  │  2. Invoke userService.authenticate(username, password)         │   ║
║  └───────────────────────┬─────────────────────────────────────────┘   ║
║                          ▼                                              ║
║  ┌─────────────────────────────────────────────────────────────────┐   ║
║  │ UserService.authenticate()                                     │   ║
║  │  1. Create LDAP context (javax.naming.ldap)                    │   ║
║  │     └──▶ LDAP Bind: cn=<username>,ou=users,dc=bytebite,...     │   ║
║  │           [LDAP_TIMEOUT FAILURE HERE]                          │   ║
║  │  2. If successful:                                              │   ║
║  │     └──▶ UserDAO.findByUsername(username)                      │   ║
║  └───────────────────────┬─────────────────────────────────────────┘   ║
║                          ▼                                              ║
║  ┌─────────────────────────────────────────────────────────────────┐   ║
║  │ UserDAO.findByUsername(username)                               │   ║
║  │  ┌──────────────────────────────────────────────────────────┐   │   ║
║  │  │ HikariCP Connection Pool                                 │   │   ║
║  │  │ (Max 20 connections, managed by Tomcat)                  │   │   ║
║  │  │                                                           │   │   ║
║  │  │ Connection conn = dataSource.getConnection();            │   │   ║
║  │  │ ┌────────────────────────────────────────────────────┐   │   │   ║
║  │  │ │ Prepared Statement Execution:                     │   │   │   ║
║  │  │ │ SELECT user_id, email, ldap_dn, is_admin         │   │   │   ║
║  │  │ │ FROM users                                        │   │   │   ║
║  │  │ │ WHERE username = ?                                │   │   │   ║
║  │  │ │                                                   │   │   │   ║
║  │  │ │ rs = stmt.executeQuery();                         │   │   │   ║
║  │  │ └────────────────────────────────────────────────────┘   │   │   ║
║  │  │                                                           │   │   ║
║  │  │ conn.close(); // Return to pool                          │   │   ║
║  │  └──────────────────────────────────────────────────────────┘   │   ║
║  │  3. Return User object (user_id, email, is_admin)                │   ║
║  └───────────────────────┬─────────────────────────────────────────┘   ║
║                          ▼                                              ║
║  ┌─────────────────────────────────────────────────────────────────┐   ║
║  │ SessionDAO.createSession(user_id, ip, userAgent)               │   ║
║  │  ┌──────────────────────────────────────────────────────────┐   │   ║
║  │  │ HikariCP Connection Pool                                 │   │   ║
║  │  │                                                           │   │   ║
║  │  │ INSERT INTO sessions                                     │   │   ║
║  │  │ (session_id, user_id, ip_address, user_agent,           │   │   ║
║  │  │  created_at, expires_at, is_active)                      │   │   ║
║  │  │ VALUES (?, ?, ?, ?, now(), now() + interval '30 min', true)  │   ║
║  │  │                                                           │   │   ║
║  │  │ UPDATE users                                             │   │   ║
║  │  │ SET last_login = now()                                   │   │   ║
║  │  │ WHERE user_id = ?                                        │   │   ║
║  │  │                                                           │   │   ║
║  │  │ conn.commit();                                           │   │   ║
║  │  └──────────────────────────────────────────────────────────┘   │   ║
║  │  4. Return session_id                                            │   ║
║  └───────────────────────┬─────────────────────────────────────────┘   ║
║                          ▼                                              ║
║  ┌─────────────────────────────────────────────────────────────────┐   ║
║  │ LoginServlet.doPost() — Response Phase                         │   ║
║  │  1. Set response cookie: JSESSIONID=<session_id>              │   ║
║  │  2. Redirect to /bytebite/menu or dashboard                   │   ║
║  │  3. Log to catalina.log: "User admin logged in"               │   ║
║  └─────────────────────────────────────────────────────────────────┘   ║
║                                                                          ║
╚═════════════════════════════════════════════════════════════════════════╝

╔═════════════════════════════════════════════════════════════════════════╗
║  SERVLET: MenuServlet → MenuService → MenuDAO                          ║
╠═════════════════════════════════════════════════════════════════════════╣
║                                                                          ║
║  GET /bytebite/api/menu (or /bytebite/menu/)                           ║
║                                                                          ║
║  ┌─────────────────────────────────────────────────────────────────┐   ║
║  │ MenuServlet.doGet(req, resp)                                    │   ║
║  │  1. Extract session from cookie                                 │   ║
║  │  2. Invoke menuService.getMenuItems()                           │   ║
║  └───────────────────────┬─────────────────────────────────────────┘   ║
║                          ▼                                              ║
║  ┌─────────────────────────────────────────────────────────────────┐   ║
║  │ MenuService.getMenuItems()                                     │   ║
║  │  1. Check session valid (from earlier filter)                  │   ║
║  │  2. Invoke menuDAO.getAllAvailableItems()                      │   ║
║  │  3. If ?failureType=db_leak: don't close connection            │   ║
║  └───────────────────────┬─────────────────────────────────────────┘   ║
║                          ▼                                              ║
║  ┌─────────────────────────────────────────────────────────────────┐   ║
║  │ MenuDAO.getAllAvailableItems()                                 │   ║
║  │  ┌──────────────────────────────────────────────────────────┐   │   ║
║  │  │ HikariCP Connection Pool                                 │   │   ║
║  │  │  ├─ Active: 15 connections                               │   │   ║
║  │  │  ├─ Idle: 5 connections                                  │   │   ║
║  │  │  └─ Max: 20 connections                                  │   │   ║
║  │  │                                                           │   │   ║
║  │  │ [DB_LEAK INJECTION: Skip conn.close()]                   │   │   ║
║  │  │                                                           │   │   ║
║  │  │ Connection conn = dataSource.getConnection();            │   │   ║
║  │  │ if (failureType == DB_LEAK) {                            │   │   ║
║  │  │   // Intentionally don't close                           │   │   ║
║  │  │ } else {                                                 │   │   ║
║  │  │   conn.close();  // Return to pool                       │   │   ║
║  │  │ }                                                         │   │   ║
║  │  │                                                           │   │   ║
║  │  │ After N requests, pool exhausted:                        │   │   ║
║  │  │ "Cannot get a connection, pool error Timeout waiting      │   │   ║
║  │  │  for idle object"                                        │   │   ║
║  │  └──────────────────────────────────────────────────────────┘   │   ║
║  │  2. Execute query:                                               │   ║
║  │     SELECT id, name, price, category, available                 │   ║
║  │     FROM menu_items                                             │   ║
║  │     WHERE available = true                                      │   ║
║  │     ORDER BY category ASC                                       │   ║
║  │  3. Parse ResultSet into MenuItem objects (16 items)            │   ║
║  │  4. Return List<MenuItem>                                       │   ║
║  └───────────────────────┬─────────────────────────────────────────┘   ║
║                          ▼                                              ║
║  ┌─────────────────────────────────────────────────────────────────┐   ║
║  │ MenuServlet.doGet() — Response Phase                           │   ║
║  │  1. Render menu as HTML table                                  │   ║
║  │  2. Include item names, prices, descriptions                  │   ║
║  │  3. Log event to analytics_events table (PAGE_VIEW)            │   ║
║  │  4. Send response with menu page                               │   ║
║  └─────────────────────────────────────────────────────────────────┘   ║
║                                                                          ║
╚═════════════════════════════════════════════════════════════════════════╝

╔═════════════════════════════════════════════════════════════════════════╗
║  SERVLET: CheckoutServlet → OrderService → OrderDAO                    ║
╠═════════════════════════════════════════════════════════════════════════╣
║                                                                          ║
║  POST /bytebite/checkout/ (with cart items)                            ║
║                                                                          ║
║  ┌─────────────────────────────────────────────────────────────────┐   ║
║  │ CheckoutServlet.doPost(req, resp)                              │   ║
║  │  1. Validate authenticated session                             │   ║
║  │  2. Parse cart items from request body                         │   ║
║  │  3. Calculate total, tax, shipping                             │   ║
║  │  4. Invoke orderService.createOrder(cartItems, user)           │   ║
║  └───────────────────────┬─────────────────────────────────────────┘   ║
║                          ▼                                              ║
║  ┌─────────────────────────────────────────────────────────────────┐   ║
║  │ OrderService.createOrder()                                     │   ║
║  │  1. Validate menu items (fetch from DB)                        │   ║
║  │  2. Calculate final amounts                                    │   ║
║  │  3. Invoke orderDAO.createOrderWithTransaction()               │   ║
║  │  4. If ?failureType=ssl: throw SSL exception                   │   ║
║  └───────────────────────┬─────────────────────────────────────────┘   ║
║                          ▼                                              ║
║  ┌─────────────────────────────────────────────────────────────────┐   ║
║  │ OrderDAO.createOrderWithTransaction()                          │   ║
║  │  ┌──────────────────────────────────────────────────────────┐   │   ║
║  │  │ HikariCP Connection Pool                                 │   │   ║
║  │  │                                                           │   │   ║
║  │  │ BEGIN TRANSACTION (autoCommit = false)                   │   │   ║
║  │  │                                                           │   │   ║
║  │  │ 1. INSERT INTO orders (WRITE LOCK on order_id)           │   │   ║
║  │  │    VALUES (user_id, order_number, total, tax, ...)       │   │   ║
║  │  │    RETURNING order_id                                    │   │   ║
║  │  │    [DEADLOCK SCENARIO: Wait for inventory lock]          │   │   ║
║  │  │                                                           │   │   ║
║  │  │ 2. INSERT INTO order_items (order_id, product_id, ...)   │   │   ║
║  │  │                                                           │   │   ║
║  │  │ 3. INSERT INTO kds_orders (order_id, kds_status=NEW)     │   │   ║
║  │  │                                                           │   │   ║
║  │  │ 4. [SSL CERTIFICATE CHECK HERE]                          │   │   ║
║  │  │    [SSL_FAILURE INJECTION: throw exception]              │   │   ║
║  │  │    "❌ SSL Certificate Validation Failed"                 │   │   ║
║  │  │                                                           │   │   ║
║  │  │ 5. If SSL fails → ROLLBACK all inserts                   │   │   ║
║  │  │    If success → COMMIT transaction                       │   │   ║
║  │  │                                                           │   │   ║
║  │  └──────────────────────────────────────────────────────────┘   │   ║
║  │  6. If committed: notify KDS service (async)                    │   ║
║  │  7. Return Order object with order_id                           │   ║
║  └───────────────────────┬─────────────────────────────────────────┘   ║
║                          ▼                                              ║
║  ┌─────────────────────────────────────────────────────────────────┐   ║
║  │ CheckoutServlet.doPost() — Response Phase                      │   ║
║  │  1. If order created successfully:                             │   ║
║  │     └──▶ Render order confirmation page                        │   ║
║  │          Show: Order ID, Total, Receipt                        │   ║
║  │  2. If error (SSL, deadlock, timeout):                         │   ║
║  │     └──▶ Render error page with failure reason                 │   ║
║  │  3. Log event: PURCHASE (if success) or CHECKOUT_FAILED        │   ║
║  │  4. Audit log: INSERT INTO api_audit_log (endpoint, status, ms)│   ║
║  └─────────────────────────────────────────────────────────────────┘   ║
║                                                                          ║
╚═════════════════════════════════════════════════════════════════════════╝

╔═════════════════════════════════════════════════════════════════════════╗
║  SERVLET: AnalyticsServlet → AnalyticsService → AnalyticsDAO           ║
╠═════════════════════════════════════════════════════════════════════════╣
║                                                                          ║
║  GET /bytebite/analytics/?action=all-time-report                       ║
║                                                                          ║
║  ┌─────────────────────────────────────────────────────────────────┐   ║
║  │ AnalyticsServlet.doGet(req, resp)                              │   ║
║  │  1. Check role: ADMIN or MANAGER only                          │   ║
║  │  2. Extract action param (all-time-report, daily, etc.)        │   ║
║  │  3. Invoke analyticsService.generateReport(action)             │   ║
║  └───────────────────────┬─────────────────────────────────────────┘   ║
║                          ▼                                              ║
║  ┌─────────────────────────────────────────────────────────────────┐   ║
║  │ AnalyticsService.generateReport()                              │   ║
║  │  1. Invoke analyticsDAO.getAllEvents()  [OOM HERE]             │   ║
║  │  2. Load all ~10,000 analytics_events into memory              │   ║
║  │  3. Parse each event's JSONB (100KB per event = 1GB RAM)       │   ║
║  │  4. Aggregate and compute statistics                           │   ║
║  │  5. Return Report object                                       │   ║
║  └───────────────────────┬─────────────────────────────────────────┘   ║
║                          ▼                                              ║
║  ┌─────────────────────────────────────────────────────────────────┐   ║
║  │ AnalyticsDAO.getAllEvents()                                    │   ║
║  │  ┌──────────────────────────────────────────────────────────┐   │   ║
║  │  │ HikariCP Connection Pool                                 │   │   ║
║  │  │                                                           │   │   ║
║  │  │ [OOM INJECTION: Load all rows into memory]               │   │   ║
║  │  │                                                           │   │   ║
║  │  │ List<AnalyticsEvent> allEvents = new ArrayList<>();      │   │   ║
║  │  │                                                           │   │   ║
║  │  │ Connection conn = dataSource.getConnection();            │   │   ║
║  │  │ Statement stmt = conn.createStatement();                 │   │   ║
║  │  │ ResultSet rs = stmt.executeQuery(                        │   │   ║
║  │  │   "SELECT * FROM analytics_events ORDER BY created_at DESC"  │   ║
║  │  │ );                                                        │   │   ║
║  │  │                                                           │   │   ║
║  │  │ while (rs.next()) {  // All 10,000 rows!                 │   │   ║
║  │  │   AnalyticsEvent event = new AnalyticsEvent();           │   │   ║
║  │  │   event.setData(rs.getString("event_data"));  // 100KB   │   │   ║
║  │  │   allEvents.add(event);  // Keep in heap                 │   │   ║
║  │  │ }                                                         │   │   ║
║  │  │                                                           │   │   ║
║  │  │ // Heap now: 10000 × 100KB = ~1GB                       │   │   ║
║  │  │ // Tomcat JVM: -Xmx512m → OutOfMemoryError!              │   │   ║
║  │  │                                                           │   │   ║
║  │  │ conn.close();                                             │   │   ║
║  │  │ return allEvents;  // Never returns; OOM exception thrown │   │   ║
║  │  └──────────────────────────────────────────────────────────┘   │   ║
║  │                                                                  │   ║
║  │  Exception: java.lang.OutOfMemoryError: Java heap space         │   ║
║  └──────────────────────────────────────────────────────────────────┘   ║
║                                                                          ║
║  ┌─────────────────────────────────────────────────────────────────┐   ║
║  │ AnalyticsServlet — Error Response                              │   ║
║  │  1. Caught exception: OutOfMemoryError                         │   ║
║  │  2. Render 500 error page                                      │   ║
║  │  3. Log stack trace with "⚠️ OOM FAILURE INJECTION" header      │   ║
║  │  4. Tomcat health check may fail (auto-restart if configured)  │   ║
║  │  5. catalina.log shows: java.lang.OutOfMemoryError: heap space │   ║
║  │  6. Reader inspects log to diagnose memory exhaustion           │   ║
║  └─────────────────────────────────────────────────────────────────┘   ║
║                                                                          ║
╚═════════════════════════════════════════════════════════════════════════╝

╔═════════════════════════════════════════════════════════════════════════╗
║  SERVLET: KDSServlet → KDSService → KDSOrderDAO                        ║
╠═════════════════════════════════════════════════════════════════════════╣
║                                                                          ║
║  POST /bytebite/api/kds (JSON: {orderId, status, failureType})         ║
║                                                                          ║
║  ┌─────────────────────────────────────────────────────────────────┐   ║
║  │ KDSServlet.doPost(req, resp)                                    │   ║
║  │  1. Validate authenticated session (KDS_OPERATOR role)          │   ║
║  │  2. Parse JSON request body                                     │   ║
║  │  3. Invoke kdsService.updateOrderStatus(orderId, status)        │   ║
║  └───────────────────────┬─────────────────────────────────────────┘   ║
║                          ▼                                              ║
║  ┌─────────────────────────────────────────────────────────────────┐   ║
║  │ KDSService.updateOrderStatus()                                 │   ║
║  │  1. Validate order exists                                      │   ║
║  │  2. Invoke kdsDAO.updateOrderStatusWithLock()                  │   ║
║  │  3. If ?failureType=deadlock: acquire locks in reverse order   │   ║
║  └───────────────────────┬─────────────────────────────────────────┘   ║
║                          ▼                                              ║
║  ┌─────────────────────────────────────────────────────────────────┐   ║
║  │ KDSOrderDAO.updateOrderStatusWithLock()                        │   ║
║  │  ┌──────────────────────────────────────────────────────────┐   │   ║
║  │  │ HikariCP Connection Pool                                 │   │   ║
║  │  │                                                           │   │   ║
║  │  │ BEGIN TRANSACTION                                         │   │   ║
║  │  │ SET TRANSACTION ISOLATION LEVEL = SERIALIZABLE            │   │   ║
║  │  │                                                           │   │   ║
║  │  │ [DEADLOCK INJECTION: Reverse lock order]                 │   │   ║
║  │  │                                                           │   │   ║
║  │  │ NORMAL: Lock orders 1st, then inventory                  │   │   ║
║  │  │ DEADLOCK: Lock inventory 1st, then orders                │   │   ║
║  │  │                                                           │   │   ║
║  │  │ 1. UPDATE kds_orders                                      │   │   ║
║  │  │    SET kds_status = ?                                    │   │   ║
║  │  │    WHERE order_id = ?                                    │   │   ║
║  │  │    FOR UPDATE;  /* Lock this row */                      │   │   ║
║  │  │                                                           │   │   ║
║  │  │ 2. UPDATE orders                                          │   │   ║
║  │  │    SET status = ?                                        │   │   ║
║  │  │    WHERE order_id = ?                                    │   │   ║
║  │  │    FOR UPDATE;  /* Try to lock order row */              │   │   ║
║  │  │    → WAIT... Checkout thread holds lock on order!        │   │   ║
║  │  │    → This thread holds lock on kds_orders                │   │   ║
║  │  │    → DEADLOCK DETECTED!                                  │   │   ║
║  │  │                                                           │   │   ║
║  │  │ Exception: org.postgresql.util.PSQLException:             │   │   ║
║  │  │           ERROR: deadlock detected                         │   │   ║
║  │  │                                                           │   │   ║
║  │  │ Automatic ROLLBACK on deadlock                            │   │   ║
║  │  └──────────────────────────────────────────────────────────┘   │   ║
║  │  Exception thrown: DeadlockException                             │   ║
║  └───────────────────────┬─────────────────────────────────────────┘   ║
║                          ▼                                              ║
║  ┌─────────────────────────────────────────────────────────────────┐   ║
║  │ KDSServlet — Deadlock Response                                 │   ║
║  │  1. Caught exception: DeadlockException                        │   ║
║  │  2. Log: "❌ DEADLOCK SCENARIO: Both threads BLOCKED"           │   ║
║  │  3. Dump thread information (show waiting threads)             │   ║
║  │  4. Render error page or generate thread dump                  │   ║
║  │  5. catalina.log shows: "ERROR: deadlock detected"             │   ║
║  │  6. Reader analyzes thread dump to understand circular wait    │   ║
║  └─────────────────────────────────────────────────────────────────┘   ║
║                                                                          ║
╚═════════════════════════════════════════════════════════════════════════╝

╔═════════════════════════════════════════════════════════════════════════╗
║              HIKARICP CONNECTION POOL LIFECYCLE                         ║
╚═════════════════════════════════════════════════════════════════════════╝

                        ┌─────────────────────────────┐
                        │    HikariCP Pool            │
                        │    (Max: 20 connections)    │
                        └────────────┬────────────────┘
                                     │
        ┌────────────────────────────┼────────────────────────────┐
        │                            │                            │
        ▼                            ▼                            ▼
   ┌─────────────┐            ┌─────────────┐           ┌──────────────┐
   │ Idle Pool   │            │ Active Conns│           │ Waiting      │
   │ (5 avail)   │            │ (15 in use) │           │ Queue        │
   └─────────────┘            └─────────────┘           └──────────────┘
        │                            │                        │
        │ getConnection()            │                        │
        ├──────────────────┐         │                        │
        │                  ▼         │                        │
        │         [Connection busy]  │                        │
        │                  │         │                        │
        │         conn.close()       │                        │
        │                  │         │                        │
        │                  ▼         │                        │
        │         [Returned to idle] │                        │
        │                  │         │                        │
        ▼                  ▼         ▼                        ▼
   ┌──────────────────────────────────────────────────────────────┐
   │ PostgreSQL Server: bytebite_db                              │
   │  • users, sessions, orders, menu_items                      │
   │  • order_items, kds_orders, analytics_events                │
   │  • application_logs, api_audit_log, user_roles              │
   └──────────────────────────────────────────────────────────────┘
```

---

## Summary

These four diagrams show:

1. **Infrastructure**: User → Tomcat (with filters/servlets) → PostgreSQL + LDAP + Logs
2. **Application Flow**: How login, menu, analytics, checkout, and KDS requests flow through the system with database transactions
3. **Database Schema**: All 11 tables with relationships (users, sessions, orders, menu_items, analytics_events, kds_orders, etc.)
4. **Servlet-Database Architecture**: How servlets connect to HikariCP pool, execute prepared statements, and handle failures (DB leak, OOM, deadlock, SSL, LDAP timeout)

Each diagram is **code-free** and suitable for book publication, focusing on failure patterns and data flow.
