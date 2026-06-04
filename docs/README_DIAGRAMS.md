# ByteBite Diagrams & Documentation Guide

Welcome to the ByteBite visual documentation suite! This directory contains professional, publication-ready diagrams for the advanced Java troubleshooting guide.

## 📂 What's in This Directory

```
docs/
├── README_DIAGRAMS.md                 (This file - navigation guide)
├── DIAGRAMS_SUMMARY.md                (Detailed diagram reference)
├── ARCHITECTURE_DIAGRAMS.md           (ASCII text-based diagrams for reference)
├── 01-infrastructure-architecture.svg (Diagram 1: Infrastructure topology)
├── 02-application-flow.svg            (Diagram 2: Request flows & session lifecycle)
├── 03-database-schema.svg             (Diagram 3: Database schema with all 11 tables)
└── 04-servlet-architecture.svg        (Diagram 4: Servlet architecture & failure points)
```

## 🎯 Quick Navigation

### For Book Writers/Editors
1. **Start with**: `DIAGRAMS_SUMMARY.md` — Complete overview of all 4 diagrams
2. **Chapter assignments**: Each diagram has "Use in Book" guidance
3. **Failure patterns**: Table of all 5 failure types with log signals

### For Developers/Maintainers
1. **Architecture details**: `01-infrastructure-architecture.svg` — System components
2. **Code flow paths**: `02-application-flow.svg` — How requests flow through app
3. **Data structures**: `03-database-schema.svg` — All 11 tables & relationships
4. **Failure injection**: `04-servlet-architecture.svg` — Where failures activate

### For Learning/Reference
1. **Text version**: `ARCHITECTURE_DIAGRAMS.md` — ASCII art diagrams (for terminal)
2. **Visual version**: Open any `.svg` file in a browser or PDF reader
3. **Failure guide**: See failure summary table in `04-servlet-architecture.svg`

## 🖼️ The 4 Diagrams at a Glance

### Diagram 1: Infrastructure Architecture
**Best for**: Understanding system components and topology
- Shows: User → Tomcat → PostgreSQL, OpenLDAP, Logging
- Key insight: Three-tier architecture with Docker orchestration
- File: `01-infrastructure-architecture.svg`

### Diagram 2: Application Request & Session Flow
**Best for**: Tracing how requests flow through the system
- Shows: 5 major flows (Login, Menu, Analytics, KDS, Checkout)
- Key insight: Filter chain, session lifecycle, transaction patterns
- File: `02-application-flow.svg`

### Diagram 3: Database Schema
**Best for**: Understanding data model and relationships
- Shows: All 11 tables, foreign keys, pre-seeded data
- Key insight: JSONB analytics_events, 10K events for OOM testing
- File: `03-database-schema.svg`

### Diagram 4: Servlet Architecture & Failure Points
**Best for**: Debugging and failure analysis
- Shows: 5 servlets, failure injection points, HikariCP pool
- Key insight: Where each failure type activates in code
- File: `04-servlet-architecture.svg`

## 🔴 The 5 Failure Types (All Diagrams)

| Failure | Trigger | Root Cause | Where to Look |
|---------|---------|-----------|---|
| **LDAP_TIMEOUT** | `?failureType=ldap_timeout` | LDAP unavailable | Diagram 4 - LOGIN FLOW |
| **DB_LEAK** | `?failureType=db_leak` | Connection not closed | Diagram 4 - MENU FLOW |
| **OOM** | `?failureType=oom` | 10K events loaded | Diagram 4 - ANALYTICS FLOW |
| **DEADLOCK** | `{"failureType":"deadlock"}` | Lock ordering mismatch | Diagram 4 - KDS/CHECKOUT |
| **SSL** | `{"failureType":"ssl"}` | Cert validation fails | Diagram 4 - CHECKOUT FLOW |

## 📖 Reading the Diagrams

### Color Coding
- **Blue**: Authentication, login, sessions, users
- **Purple**: Roles, permissions, access control
- **Orange**: Tomcat, servlets, request handling
- **Green**: Database, transactions, queries
- **Red**: Failure injection points, errors
- **Yellow**: Summaries, notes, key information

### Flow Arrows
- **Black arrows (→)**: Normal data/control flow
- **Red arrows (→)**: Failure paths
- **Dashed lines**: Optional connections or async operations

### Annotations
- **🔑**: Primary key
- **🔗**: Foreign key relationship
- **⚠️**: Warning or failure injection point
- **❌**: Error or exception
- **📊**: Analytics or reporting

## 🔍 Finding Specific Information

### Want to find...
- **How requests flow?** → See Diagram 2
- **Database tables?** → See Diagram 3
- **Failure injection code?** → See Diagram 4
- **System architecture?** → See Diagram 1
- **What a specific table does?** → Search Diagram 3
- **Which servlet handles login?** → See Diagram 4 - Flow ①

### Looking for a specific failure?

1. **LDAP timeout?**
   - Diagram 2: LOGIN Flow section
   - Diagram 4: LoginServlet flow ①
   - File: `src/main/java/.../LoginServlet.java:141`

2. **Database connection leak?**
   - Diagram 2: MENU Flow section
   - Diagram 4: MenuServlet flow ②
   - File: `src/main/java/.../MenuServlet.java:79`

3. **Out of memory?**
   - Diagram 2: ANALYTICS Flow section
   - Diagram 4: AnalyticsServlet flow ③
   - File: `src/main/java/.../AnalyticsServlet.java:96`

4. **Deadlock?**
   - Diagram 2: KDS/CHECKOUT Flow section
   - Diagram 4: KDSServlet flow ④ / CheckoutServlet flow ⑤
   - File: `src/main/java/.../KDSServlet.java:55`

5. **SSL certificate failure?**
   - Diagram 2: CHECKOUT Flow section
   - Diagram 4: CheckoutServlet flow ⑤
   - File: `src/main/java/.../CheckoutServlet.java:68`

## 📚 Using Diagrams in the Book

### Chapter Mapping

| Chapter | Diagram | Focus |
|---------|---------|-------|
| 1. System Architecture | 1 | Component topology, deployment |
| 2. Request Flow & Routing | 2 | How requests move through system |
| 3. Data Model | 3 | Database schema, relationships |
| 4. LDAP Auth Failures | 4, 2 | Login flow, auth timeout |
| 5. Connection Pool Issues | 4, 2 | Database leaks, pool exhaustion |
| 6. Memory Problems | 4, 2 | OOM errors, unbounded queries |
| 7. Thread Contention | 4, 2 | Deadlocks, lock ordering |
| 8. TLS/SSL Issues | 4, 2 | Certificate validation, transport |

### Export for Print

All diagrams are **SVG format** (scalable vector graphics):
- **Print quality**: Export to PDF at 300+ DPI
- **Screen quality**: Use SVG directly in web/ePub
- **Editability**: Open in Inkscape or Illustrator if customization needed

## 🔧 Maintaining the Diagrams

### When to Update

- Code changes that affect servlet flow
- New/removed database tables
- Changed connection pool configuration
- Different failure injection points

### How to Update

1. SVG files can be edited with:
   - **Inkscape** (free, open source)
   - **Adobe Illustrator** (professional)
   - **Online**: **draw.io** (free web editor)

2. Keep text-based diagrams in sync with SVG files
3. Update DIAGRAMS_SUMMARY.md with any changes
4. Regenerate from source code documentation

### Tools & Resources

- **SVG Editor**: https://draw.io
- **Conversion**: Use Inkscape to export to PDF/PNG
- **Version Control**: Store SVGs in git (they're text-based!)

## 📊 Statistics

- **Total diagrams**: 4 (all SVG format)
- **Total tables shown**: 11 (all PostgreSQL tables)
- **Total servlets shown**: 6
- **Failure injection points**: 5
- **Color palette**: Consistent across all diagrams
- **Text annotations**: Yes (no code snippets)
- **Print-ready**: Yes (600+ DPI capable)

## ✅ Quality Checklist

- [x] All 4 diagrams SVG-based and scalable
- [x] Consistent color coding across diagrams
- [x] All 11 database tables documented
- [x] All 5 failure types marked
- [x] All 6 servlets included
- [x] Cross-references between diagrams
- [x] Code line numbers for failures
- [x] Log signal patterns documented
- [x] Pre-seeded data examples shown
- [x] Print-optimized (no fine details)
- [x] Screen-optimized (clear text, readable colors)
- [x] Accessible (good contrast, labeled colors)

## 🔗 Related Documentation

- **CLAUDE.md**: Project context, failure injection code lines
- **README.md**: Quick start, overview
- **TESTING.md**: How to trigger failures and analyze logs
- **DEPLOYMENT.md**: Production deployment notes
- **Source Code**: Actual servlet implementations with comments

## 💡 Pro Tips

1. **Print tip**: Use color printing for the diagrams — colors are meaningful!
2. **Web tip**: SVG scales perfectly at any size — no quality loss
3. **Reference tip**: Bookmark the failure table in Diagram 4
4. **Debug tip**: Keep Diagram 2 & 4 side-by-side when analyzing logs
5. **Teaching tip**: Start with Diagram 1, then progress to Diagram 4

## 🆘 Troubleshooting Diagrams

- **Can't open SVG?** Try any modern browser (Chrome, Firefox, Safari, Edge)
- **Text too small?** Zoom in with browser zoom (Ctrl/Cmd + +)
- **Need to edit?** Use draw.io or download Inkscape (free)
- **Print looks bad?** Check that color is enabled in print settings
- **Missing details?** Zoom to 100% or open at full browser size

---

**Last Updated**: June 2026  
**Status**: ✅ Complete & Production Ready  
**Format**: 4 SVG Diagrams + Markdown Documentation  
**Audience**: Book authors, developers, engineers  
**License**: Educational use (same as ByteBite project)
