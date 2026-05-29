# Blueprint: ByteBite Application Layer & Screen Flows

This document details the functional design of the simple student/staff web layers inside the Java Servlet application (`ByteBiteApp.war`) running on Tomcat 10.1 (JDK 17).

## 1. Web Page & Servlet Architecture
The application uses basic Jakarta Servlet views and standard HTML pages. No heavy frontend frameworks are permitted.

### Screen 1: Login / Staff Portal (`/login`)
* **UI:** Simple HTML form demanding Username and Password.
* **Backend:** Maps to `LoginServlet`. Authenticates users directly against the local OpenLDAP container.

### Screen 2: Menu & Ordering Page (`/menu`)
* **UI:** Tabular display showing food items, prices, and a "Place Order" button.
* **Backend:** Maps to `MenuServlet`. Queries the PostgreSQL database `menu_items` table to render data dynamically.

### Screen 3: Payment Checkout (`/checkout`)
* **UI:** Simple total summary confirmation screen with a "Pay Now" button.
* **Backend:** Maps to `CheckoutServlet`. Executes an outbound HTTPS POST request to an external third-party mockup API gateway.

### Screen 4: Order History & Analytics Dashboard (`/analytics`)
* **UI:** Basic reporting page with an "Execute All-Time Report" button.
* **Backend:** Maps to `AnalyticsServlet`. Executes queries against the `orders` table in PostgreSQL.

### Screen 5: Kitchen Display System (`/kds`)
* **UI:** Operational dashboard displaying pending orders. Contains a button to "Complete Top Order".
* **Backend:** Maps to `KDSServlet`. Updates order rows status and recalculates item values concurrently.