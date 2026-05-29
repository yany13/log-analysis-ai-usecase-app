# Blueprint: ByteBite Application Layer & Screen Flows UI Design
 
This document details the functional design of the modern, responsive web layers inside the Java Servlet application (`ByteBiteApp.war`) running on Tomcat 10.1 (JDK 17).

## 1. Web Page & Frontend UI Architecture
To maintain low deployment overhead and strict token-efficiency, the application relies entirely on native Jakarta Servlet views rendering vanilla HTML5. **No heavy JavaScript frameworks (React, Angular, Vue) are permitted.** However, the UI must look like a high-end, elegant modern restaurant application rather than unstyled text.

### Universal UI & Styling Guardrails:
* **Aesthetics:** Use a unified, modern dark/light minimalist theme suitable for a premium restaurant (e.g., deep charcoal, warm cream, and gold or emerald accents).
* **Styling Delivery:** All styling must be written using a single, cohesive, modern embedded CSS block or a single `/css/styles.css` asset loaded inside the `<head>`. Use modern CSS layouts (Flexbox, CSS Grid) with elegant typography (e.g., system-ui, Inter, or serif headlines for a premium feel), clean input cards, subtle box-shadows, and smooth micro-interactions on buttons.
* **Component Structure:** Every page must render an elegant top navigation bar featuring the "ByteBite" branding, current route markers, and quick contextual toggles to trigger/reset the targeted backend failure simulation vectors.