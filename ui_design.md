# Blueprint: ByteBite Application Layer & Screen Flows - UI Design

This document details the functional design of the modern, responsive web layers inside the Java Servlet application (`ByteBiteApp.war`) running on Tomcat 11 (JDK 21).

## 1. Web Page & Frontend UI Architecture

To maintain low deployment overhead and strict token-efficiency, the application relies entirely on native Jakarta Servlet views rendering vanilla HTML5. **No heavy JavaScript frameworks (React, Angular, Vue) are permitted.** However, the UI must look like a high-end, elegant modern restaurant application rather than unstyled text.

### Universal UI & Styling Guardrails:

* **Aesthetics:** Use a unified, modern dark/light minimalist theme suitable for a premium restaurant (e.g., deep charcoal, warm cream, and gold or emerald accents).
* **Styling Delivery:** All styling must be written using a single, cohesive, modern embedded CSS block or a single `/css/styles.css` asset loaded inside the `<head>`. Use modern CSS layouts (Flexbox, CSS Grid) with elegant typography (e.g., system-ui, Inter, or serif headlines for a premium feel), clean input cards, subtle box-shadows, and smooth micro-interactions on buttons.
* **Component Structure:** Every page must render an elegant top navigation bar featuring the "ByteBite" branding, current route markers, and quick contextual toggles to trigger/reset the targeted backend failure simulation vectors.

## 2. Page Layouts & Screen Flows

### Screen 1: Login / Authentication Portal (`/login`)

**Purpose:** User authentication entry point  
**Servlet:** `LoginServlet`  

**UI Components:**
- Centered card layout with ByteBite branding
- Username input field
- Password input field
- "Sign In" button
- Error message display area
- "Forgot Password?" link (optional)

**Backend Integration:**
- Form POST to `/login`
- Validates against OpenLDAP or fallback database
- Sets secure session cookie on success
- Redirects to `/menu` on successful auth

**Styling:** Premium dark card with gold accent borders, smooth focus states on inputs

---

### Screen 2: Menu & Ordering Page (`/menu`)

**Purpose:** Browse menu items and add to shopping cart  
**Servlet:** `MenuServlet`  

**UI Components:**
- Top navigation bar with user name, logout button
- Menu category tabs (Appetizers, Main, Dessert, Beverages)
- Item cards displaying: name, description, price, image placeholder
- "Add to Cart" button per item
- Cart summary badge (count + total)
- Shopping cart drawer/modal

**Backend Integration:**
- GET `/api/menu` - Loads JSON menu data
- POST `/api/cart` - Adds items to cart (localStorage client-side)
- Batch query against `menu_items` table

**Failure Injection Vector (DB_Connection_Leak):**
- Intentional: Missing connection close in `MenuServlet` causes connection pool exhaustion
- Expected Log: `java.sql.SQLException: Connection is not available, request timed out after Xms`

**Styling:** Responsive grid layout, hover state on cards, smooth add-to-cart animations

---

### Screen 3: Checkout & Payment (`/checkout`)

**Purpose:** Review order and process payment  
**Servlet:** `CheckoutServlet`  

**UI Components:**
- Order summary: items list with quantities and prices
- Subtotal, tax, total calculation display
- Customer info form (name, email, address, phone)
- Payment method selector (Credit Card, Apple Pay, etc.)
- "Complete Order" button
- Order confirmation message

**Backend Integration:**
- GET `/checkout` - Displays form
- POST `/api/orders` - Creates order record
- External HTTPS POST to payment gateway mock
- Stores in `orders` and `order_items` tables

**Failure Injection Vector (SSL_Handshake_Failure):**
- Intentional: HTTPS call to self-signed certificate endpoint
- Expected Log: `javax.net.ssl.SSLHandshakeException: PKIX path building failed`

**Styling:** Clean form layout, prominent CTA button, success state confirmation

---

### Screen 4: Analytics & Reporting Dashboard (`/analytics`)

**Purpose:** View order history and system health metrics  
**Servlet:** `AnalyticsServlet`  

**UI Components:**
- Top metric cards: total orders, revenue, avg order value
- Date range selector (Today, Week, Month, All-Time)
- "Generate Report" button
- Analytics chart placeholder (D3.js not used; static/table display)
- Health status panel showing database and LDAP connectivity
- Event log table (recent events)

**Backend Integration:**
- GET `/api/analytics` - Loads aggregate data
- Queries `analytics_events` and `orders` tables
- Pagination support for large datasets

**Failure Injection Vector (OOM_Heap_Exhaustion):**
- Intentional: All-time report loops all results into unmanaged List without pagination
- Expected Log: `java.lang.OutOfMemoryError: Java heap space`

**Styling:** Dashboard card layout, metric highlight boxes, responsive table

---

### Screen 5: Kitchen Display System (`/kds`)

**Purpose:** Operational view for kitchen staff to manage orders  
**Servlet:** `KDSServlet`  

**UI Components:**
- Order ticket cards (status: New, In Progress, Ready)
- Ticket details: order ID, customer name, items, special instructions
- "Mark Complete" button per ticket
- Real-time order queue visualization
- Timer showing order age
- Audio/visual alert for new orders

**Backend Integration:**
- GET `/api/kds` - Loads pending orders
- POST `/api/kds` - Updates order status
- WebSocket ready (not implemented; polling acceptable)
- Queries `kds_orders` and `order_items` tables

**Failure Injection Vector (Thread_Deadlock):**
- Intentional: Inverted synchronized block lock ordering between order and inventory updates
- Expected Log: Tomcat worker threads hanging; `jstack` shows `BLOCKED` state

**Styling:** Large readable ticket cards, color-coded status badges, prominent action buttons

---

## 3. Navigation Flow Diagram

```
┌─────────────┐
│   Login     │ (Initial Auth)
│  `/login`   │
└──────┬──────┘
       │ (Success)
       ↓
┌─────────────────────────────┐
│      Menu Page              │
│      `/menu`                │
│  ┌─ Add to Cart ─┐          │
└──┼──────────────┼──────────┬┘
   │              │          │
   │              │          └─→ Cart Icon (drawer/modal)
   │              │
   └──────────────┘
      (Proceed to Checkout)
         │
         ↓
┌────────────────────────┐
│   Checkout Page        │
│   `/checkout`          │
│  [Review & Confirm]    │
└──────────┬─────────────┘
           │ (Submit Order)
           ↓
        Success
           │
    ┌──────┴──────┐
    │             │
    ↓             ↓
┌─────────┐  ┌──────────┐
│Analytics│  │   KDS    │
│`/analy..│  │  `/kds`  │
└─────────┘  └──────────┘
```

## 4. Error & Validation Handling

- **Client-Side Validation:** Basic HTML5 validation (required fields, email format)
- **Server-Side Validation:** Input sanitization, business logic checks
- **Error Messages:** User-friendly messages without stack traces
- **LDAP Failures:** Graceful fallback to local user database
- **Database Errors:** Logged to catalina.out; user sees generic "Service unavailable" message

## 5. CSS & Responsive Design Strategy

**Color Palette:**
- Primary: Charcoal (#2C2C2C) - Dark background
- Accent: Gold (#D4AF37) - Premium highlight
- Secondary: Cream (#F5F5F0) - Light cards
- Status: Green (#2ECC40) - Success, Red (#FF4136) - Error

**Typography:**
- Headlines: Georgia or system serif
- Body: system-ui, -apple-system, sans-serif
- Code: Monospace for data display

**Layouts:**
- Mobile-first responsive design
- Flexbox for navigation, card layouts
- CSS Grid for dashboard metrics
- Smooth transitions and hover states

**Performance:**
- Single CSS file load
- Minimal JavaScript (vanilla ES6+)
- LocalStorage for cart state
- No external CDN dependencies

---

**Last Updated:** May 2026  
**Status:** Complete UI/UX Specification  
**Implementation:** Ready for Servlet page rendering
