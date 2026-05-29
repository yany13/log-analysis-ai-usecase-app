# Blueprint: Failure Injection & Troubleshooting Use Cases

This document describes how each of the 5 production failures must be intentionally programmed into the ByteBite application layer to generate clean, analytical investigative evidence in `catalina.out`.

## Use Case 1: DB_Connection_Leak (Database Pool Exhaustion)
* **Trigger Location:** `/menu` (`MenuServlet`)
* **Code Mechanism:** Write a path where a database `Connection` object is successfully requested from the HikariCP/C3P0 pool but bypassed by a missing `try-with-resources` or explicit close block.
* **Expected Log Output:** `java.sql.SQLException: Connection is not available, request timed out after Xms.` followed by HikariCP pool status metrics showing active connections maxed out.

## Use Case 2: LDAP_Auth Issues (Authentication Failures/Timeouts)
* **Trigger Location:** `/login` (`LoginServlet`)
* **Code Mechanism:** Create a switch mechanism where an incorrect connection timeout property or invalid Context configuration string is passed to `javax.naming.directory.InitialDirContext`.
* **Expected Log Output:** `javax.naming.NamingException` or `javax.naming.CommunicationException: connection timed out` accompanied by deep identity system connection traces.

## Use Case 3: OOM Issues (Java Heap Exhaustion)
* **Trigger Location:** `/analytics` (`AnalyticsServlet`)
* **Code Mechanism:** When the all-time report is run, loop the DB cursor results into an unmanaged, globally scoped `java.util.List` or `HashMap` in memory without pagination or limits until the JVM allocation limit is crossed.
* **Expected Log Output:** `java.lang.OutOfMemoryError: Java heap space` with a termination stack trace showing thread death.

## Use Case 4: Thread_Leak (Blocked/Stuck/Hogging Threads)
* **Trigger Location:** `/kds` (`KDSServlet`)
* **Code Mechanism:** Code two nested `synchronized` blocks that update order entries and inventory rows in inverted sequence across two simultaneous client threads, intentionally provoking a structural Deadlock.
* **Expected Log Output:** Tomcat executive worker threads spinning or locking permanently, leaving requests hanging without completing. The log will remain silent for these threads until a thread dump (`jstack`) shows `java.lang.Thread.State: BLOCKED`.

## Use Case 5: SSL Issues (Handshake/Truststore Failures)
* **Trigger Location:** `/checkout` (`CheckoutServlet`)
* **Code Mechanism:** Direct the HTTP client to make a call to an HTTPS endpoint secured by a self-signed or invalid certificate, ensuring that the local JDK `cacerts` or Tomcat custom truststore explicitly rejects the validation chain.
* **Expected Log Output:** `javax.net.ssl.SSLHandshakeException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target`.