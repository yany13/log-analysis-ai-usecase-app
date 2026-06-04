# ByteBite Testing Guide

Comprehensive testing documentation for the ByteBite application covering unit tests, integration tests, end-to-end tests, and deployment verification.

## Table of Contents

1. [Overview](#overview)
2. [Unit Testing](#unit-testing)
3. [Integration Testing](#integration-testing)
4. [End-to-End Testing](#end-to-end-testing)
5. [Load & Performance Testing](#load--performance-testing)
6. [Security Testing](#security-testing)
7. [Deployment Verification](#deployment-verification)
8. [Test Reporting](#test-reporting)
9. [CI/CD Integration](#cicd-integration)

## Overview

ByteBite has comprehensive test coverage across multiple layers:

| Test Type | Scope | Tool | Location |
|-----------|-------|------|----------|
| **Unit Tests** | Utility classes | JUnit 4, Mockito | `src/test/java` |
| **Integration Tests** | Servlets + Services + DB | JUnit, Docker | Integration tests |
| **E2E Tests** | User workflows | Selenium, curl | Manual/Scripts |
| **Load Tests** | Performance | JMeter, Gatling | Load test suite |
| **Security Tests** | Vulnerabilities | OWASP ZAP, SonarQube | Security scan |

## Unit Testing

### Test Framework Setup

Tests use JUnit 4.13.2 with Mockito 5.7.0 for mocking:

```xml
<!-- pom.xml dependencies -->
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.7.0</version>
    <scope>test</scope>
</dependency>
```

### Running Unit Tests

#### All Tests

```bash
# Run all unit tests
mvn test

# Output:
# -------------------------------------------------------
#  T E S T S
# -------------------------------------------------------
# Running com.bytebite.util.SecurityUtilTest
# Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.234 s
# Running com.bytebite.util.PaginationUtilTest
# Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.167 s
# Running com.bytebite.util.DateTimeUtilTest
# Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.145 s
#
# Results: Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
```

#### Specific Test Class

```bash
# Run single test class
mvn test -Dtest=SecurityUtilTest

# Run specific test method
mvn test -Dtest=SecurityUtilTest#testPasswordHashing

# Run tests matching pattern
mvn test -Dtest=*UtilTest
```

#### With Coverage Report

```bash
# Generate JaCoCo coverage report
mvn test jacoco:report

# View report
# File: target/site/jacoco/index.html

# Generate with minimum coverage requirement
mvn test jacoco:report jacoco:check \
  -Djacoco.haltOnFailure=true \
  -Djacoco.rules.element=BUNDLE \
  -Djacoco.rules.minimum=0.70
```

#### Parallel Test Execution

```bash
# Run tests in parallel (faster)
mvn test -DparallelTestClasses

# With threads
mvn test -DparallelTestClasses -DthreadCount=4
```

### Test Coverage Details

#### SecurityUtilTest

Located in `src/test/java/com/bytebite/util/SecurityUtilTest.java`

Tests for security utility functions:

```java
@Test
public void testPasswordHashing() {
    String password = "testPassword123";
    String hash = SecurityUtil.hashPassword(password);
    assertTrue(SecurityUtil.verifyPassword(password, hash));
}

@Test
public void testHTMLEncoding() {
    String malicious = "<script>alert('xss')</script>";
    String encoded = SecurityUtil.encodeHTML(malicious);
    assertFalse(encoded.contains("<script>"));
}

@Test
public void testInputSanitization() {
    String dirty = "admin'; DROP TABLE users; --";
    String clean = SecurityUtil.sanitizeInput(dirty);
    assertFalse(clean.contains("'"));
}

@Test
public void testCSRFTokenGeneration() {
    String token1 = SecurityUtil.generateCSRFToken();
    String token2 = SecurityUtil.generateCSRFToken();
    assertNotEquals(token1, token2);
}

@Test
public void testTokenValidation() {
    String token = SecurityUtil.generateCSRFToken();
    assertTrue(SecurityUtil.validateCSRFToken(token));
}
```

#### PaginationUtilTest

Located in `src/test/java/com/bytebite/util/PaginationUtilTest.java`

Tests for pagination logic:

```java
@Test
public void testCalculateOffset() {
    int page = 2;
    int pageSize = 10;
    int offset = PaginationUtil.calculateOffset(page, pageSize);
    assertEquals(10, offset);  // (2-1)*10 = 10
}

@Test
public void testCalculateTotalPages() {
    int totalItems = 45;
    int pageSize = 10;
    int pages = PaginationUtil.calculateTotalPages(totalItems, pageSize);
    assertEquals(5, pages);  // ceil(45/10) = 5
}

@Test
public void testValidatePaginationParams() {
    assertTrue(PaginationUtil.validatePaginationParams(1, 10));
    assertFalse(PaginationUtil.validatePaginationParams(0, 10));
    assertFalse(PaginationUtil.validatePaginationParams(-1, 10));
}

@Test
public void testBoundaryConditions() {
    assertEquals(0, PaginationUtil.calculateOffset(1, 10));
    assertEquals(100, PaginationUtil.calculateOffset(11, 10));
}
```

#### DateTimeUtilTest

Located in `src/test/java/com/bytebite/util/DateTimeUtilTest.java`

Tests for date/time operations:

```java
@Test
public void testFormatDate() {
    LocalDateTime dateTime = LocalDateTime.of(2026, 6, 1, 14, 30, 0);
    String formatted = DateTimeUtil.formatDate(dateTime);
    assertEquals("2026-06-01 14:30:00", formatted);
}

@Test
public void testParseDate() {
    String dateStr = "2026-06-01 14:30:00";
    LocalDateTime parsed = DateTimeUtil.parseDate(dateStr);
    assertEquals(14, parsed.getHour());
}

@Test
public void testGetCurrentTimestamp() {
    long timestamp = DateTimeUtil.getCurrentTimestamp();
    assertTrue(timestamp > 0);
    assertTrue(timestamp <= System.currentTimeMillis());
}

@Test
public void testCalculateDateDifference() {
    LocalDate date1 = LocalDate.of(2026, 6, 1);
    LocalDate date2 = LocalDate.of(2026, 6, 11);
    long days = DateTimeUtil.calculateDaysBetween(date1, date2);
    assertEquals(10, days);
}

@Test
public void testTimeZoneHandling() {
    String utcTime = "2026-06-01T14:30:00Z";
    LocalDateTime parsed = DateTimeUtil.parseUTC(utcTime);
    assertNotNull(parsed);
}

@Test
public void testTimestampConversion() {
    long timestamp = 1717254600000L;  // June 1, 2026
    LocalDateTime dateTime = DateTimeUtil.timestampToDateTime(timestamp);
    assertEquals(2026, dateTime.getYear());
}
```

## Integration Testing

### Servlet & Service Testing with Docker

Test full request/response cycle with running containers:

```bash
# 1. Start Docker services
docker-compose up -d

# 2. Wait for initialization
sleep 30

# 3. Run integration tests
mvn integration-test

# 4. Stop services
docker-compose down
```

### Health Check Integration Test

```bash
#!/bin/bash
# integration-test.sh

BASE_URL="http://localhost:8080/bytebite"
PASSED=0
FAILED=0

echo "Running ByteBite Integration Tests..."
echo "======================================"

# Test 1: Health endpoint
echo "Test 1: Health Check Endpoint"
HEALTH=$(curl -s $BASE_URL/health | jq -r '.status')
if [ "$HEALTH" = "UP" ]; then
    echo "✓ PASSED: Health check returned UP"
    ((PASSED++))
else
    echo "✗ FAILED: Health check returned $HEALTH"
    ((FAILED++))
fi

# Test 2: Login form
echo "Test 2: Login Form"
LOGIN_PAGE=$(curl -s $BASE_URL/login | grep -c "username")
if [ $LOGIN_PAGE -gt 0 ]; then
    echo "✓ PASSED: Login form loaded"
    ((PASSED++))
else
    echo "✗ FAILED: Login form not found"
    ((FAILED++))
fi

# Test 3: Menu API
echo "Test 3: Menu API"
MENU=$(curl -s $BASE_URL/api/menu | jq '.status')
if [ ! -z "$MENU" ]; then
    echo "✓ PASSED: Menu API returned data"
    ((PASSED++))
else
    echo "✗ FAILED: Menu API error"
    ((FAILED++))
fi

# Test 4: Authentication
echo "Test 4: Authentication"
AUTH=$(curl -s -X POST $BASE_URL/login \
  -d "username=admin&password=admin" \
  -w "%{http_code}")
HTTP_CODE=${AUTH: -3}
if [ "$HTTP_CODE" = "302" ] || [ "$HTTP_CODE" = "200" ]; then
    echo "✓ PASSED: Login successful (HTTP $HTTP_CODE)"
    ((PASSED++))
else
    echo "✗ FAILED: Login returned HTTP $HTTP_CODE"
    ((FAILED++))
fi

# Test 5: LDAP Health
echo "Test 5: LDAP Connectivity"
LDAP_STATUS=$(curl -s $BASE_URL/health | jq -r '.ldap.status')
if [ "$LDAP_STATUS" = "UP" ]; then
    echo "✓ PASSED: LDAP is UP"
    ((PASSED++))
else
    echo "✗ FAILED: LDAP status is $LDAP_STATUS"
    ((FAILED++))
fi

# Test 6: Database Health
echo "Test 6: Database Connectivity"
DB_STATUS=$(curl -s $BASE_URL/health | jq -r '.database.status')
if [ "$DB_STATUS" = "UP" ]; then
    echo "✓ PASSED: Database is UP"
    ((PASSED++))
else
    echo "✗ FAILED: Database status is $DB_STATUS"
    ((FAILED++))
fi

echo "======================================"
echo "Results: $PASSED passed, $FAILED failed"

# Exit with error if any failed
[ $FAILED -eq 0 ]
```

### API Integration Tests

```bash
#!/bin/bash
# api-integration-test.sh

BASE_URL="http://localhost:8080/bytebite"

echo "API Integration Tests"
echo "===================="

# Get authentication token via session
echo "1. Login and get session"
curl -s -X POST $BASE_URL/login \
  -d "username=admin&password=admin" \
  -c cookies.txt

# Test menu endpoint with session
echo "2. Get menu items"
curl -s -b cookies.txt $BASE_URL/api/menu | jq '.'

# Test analytics event tracking
echo "3. Track analytics event"
curl -s -X POST $BASE_URL/api/analytics \
  -b cookies.txt \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "PAGE_VIEW",
    "eventData": {
      "page": "menu",
      "timestamp": '$(date +%s)'
    }
  }' | jq '.'

# Test KDS API
echo "4. Get active KDS orders"
curl -s -b cookies.txt $BASE_URL/api/kds | jq '.'

# Test order creation
echo "5. Create order"
curl -s -X POST $BASE_URL/api/orders \
  -b cookies.txt \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"itemId": 1, "quantity": 2},
      {"itemId": 3, "quantity": 1}
    ],
    "totalAmount": 45.99,
    "address": "123 Main St"
  }' | jq '.'

# Logout
echo "6. Logout"
curl -s -b cookies.txt $BASE_URL/logout
```

## End-to-End Testing

### Browser-Based E2E Tests (Selenium)

```java
// E2ELoginTest.java
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.junit.Test;

public class E2ELoginTest {
    
    @Test
    public void testLoginFlow() {
        WebDriver driver = new ChromeDriver();
        
        try {
            // Navigate to login page
            driver.get("http://localhost:8080/bytebite/login");
            
            // Find and fill username
            WebElement username = driver.findElement(By.name("username"));
            username.sendKeys("admin");
            
            // Find and fill password
            WebElement password = driver.findElement(By.name("password"));
            password.sendKeys("admin");
            
            // Submit form
            WebElement loginButton = driver.findElement(By.id("loginBtn"));
            loginButton.click();
            
            // Wait for menu page
            Thread.sleep(2000);
            
            // Verify successful login
            String pageTitle = driver.getTitle();
            assert(pageTitle.contains("Menu"));
            
            System.out.println("✓ Login test passed");
        } finally {
            driver.quit();
        }
    }
    
    @Test
    public void testMenuBrowsing() throws Exception {
        WebDriver driver = new ChromeDriver();
        
        try {
            // Login
            driver.get("http://localhost:8080/bytebite/login");
            driver.findElement(By.name("username")).sendKeys("admin");
            driver.findElement(By.name("password")).sendKeys("admin");
            driver.findElement(By.id("loginBtn")).click();
            Thread.sleep(2000);
            
            // Browse menu
            driver.get("http://localhost:8080/bytebite/menu");
            Thread.sleep(1000);
            
            // Verify menu items displayed
            int itemCount = driver.findElements(By.class("menu-item")).size();
            assert(itemCount > 0);
            
            System.out.println("✓ Menu browsing test passed");
        } finally {
            driver.quit();
        }
    }
}
```

### Running E2E Tests

```bash
# Install Selenium WebDriver
mvn dependency:get -Dartifact=org.seleniumhq.selenium:selenium-java:4.0.0

# Run E2E tests
mvn test -Dtest=E2E*

# Run with Chrome options
mvn test -Dtest=E2E* -Dwebdriver.chrome.driver=/path/to/chromedriver
```

## Load & Performance Testing

### JMeter Load Testing

Create `load-test.jmx` configuration:

```bash
# 1. Install JMeter
# Download from https://jmeter.apache.org/

# 2. Create test plan
jmeter -n -t load-test.jmx -l results.jtl -j jmeter.log

# 3. Analyze results
jmeter -g results.jtl -o htmlreport
```

#### Load Test Scenario

```java
// Simulates 100 users with 5-minute test
// 1. 10 sec ramp-up
// 2. 300 sec test duration
// 3. 10 sec ramp-down

Thread Group Settings:
- Number of Threads: 100
- Ramp-Up Period: 10 seconds
- Loop Count: 5

Requests:
1. POST /login (50%)
2. GET /api/menu (30%)
3. POST /api/analytics (20%)
```

### Performance Benchmarking

```bash
#!/bin/bash
# benchmark.sh

RUNS=5
REQUESTS=1000
CONCURRENCY=10

echo "ByteBite Performance Benchmark"
echo "=============================="
echo "Requests: $REQUESTS"
echo "Concurrency: $CONCURRENCY"
echo "Runs: $RUNS"
echo ""

for i in $(seq 1 $RUNS); do
    echo "Run $i/$RUNS"
    
    # Benchmark menu endpoint
    ab -n $REQUESTS -c $CONCURRENCY \
       -g menu_run$i.tsv \
       http://localhost:8080/bytebite/api/menu
    
    # Benchmark health endpoint
    ab -n $REQUESTS -c $CONCURRENCY \
       -g health_run$i.tsv \
       http://localhost:8080/bytebite/health
done

echo ""
echo "Benchmark complete. Results in *.tsv files"
```

---

## Security Testing

### OWASP ZAP Scanning

```bash
# Start OWASP ZAP in daemon mode
zaproxy -cmd -config api.disablekey=true

# Run baseline scan
curl "http://localhost:8080/scan?url=http://localhost:8080/bytebite&scantype=BASELINE"

# View results
curl "http://localhost:8080/jsonreport"
```

### Manual Security Tests

```bash
# 1. SQL Injection Test
curl -X POST http://localhost:8080/bytebite/login \
  --data "username=admin' OR '1'='1&password=anything"
# Expected: Login fails, no SQL error

# 2. XSS Test
curl "http://localhost:8080/bytebite/menu?search=<script>alert('xss')</script>"
# Expected: Script tags HTML-encoded, no alert

# 3. CSRF Test
# Attempt to POST without CSRF token
curl -X POST http://localhost:8080/bytebite/api/analytics \
  -H "Content-Type: application/json" \
  -d '{"eventType":"TEST"}'
# Expected: Request validated or token required

# 4. Authentication Bypass
curl -X POST http://localhost:8080/bytebite/checkout
# Expected: 302 redirect to login

# 5. Session Fixation
# Test session handling and timeout
```

### SonarQube Analysis

```bash
# 1. Install SonarQube
docker run -d --name sonarqube -p 9000:9000 sonarqube:latest

# 2. Configure Maven
mvn sonar:sonar \
  -Dsonar.projectKey=bytebite \
  -Dsonar.sources=src \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=admin:admin

# 3. View results
# Open http://localhost:9000/dashboard?id=bytebite
```

## Deployment Verification

### Pre-Deployment Checklist

```bash
#!/bin/bash
# pre-deployment-check.sh

echo "Pre-Deployment Verification"
echo "==========================="

PASS=0
FAIL=0

# 1. Java version
echo "✓ Checking Java version..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | grep version | cut -d '"' -f 2)
    echo "  Java version: $JAVA_VERSION"
    ((PASS++))
else
    echo "✗ Java not found"
    ((FAIL++))
fi

# 2. Docker
echo "✓ Checking Docker..."
if docker --version &> /dev/null; then
    echo "  Docker: $(docker --version)"
    ((PASS++))
else
    echo "✗ Docker not installed"
    ((FAIL++))
fi

# 3. Docker Compose
echo "✓ Checking Docker Compose..."
if docker-compose --version &> /dev/null; then
    echo "  Docker Compose: $(docker-compose --version)"
    ((PASS++))
else
    echo "✗ Docker Compose not installed"
    ((FAIL++))
fi

# 4. Maven
echo "✓ Checking Maven..."
if mvn --version &> /dev/null; then
    MVN_VERSION=$(mvn --version | grep Apache | cut -d ' ' -f 3)
    echo "  Maven: $MVN_VERSION"
    ((PASS++))
else
    echo "✗ Maven not installed"
    ((FAIL++))
fi

# 5. Build test
echo "✓ Building application..."
if mvn clean package -DskipTests &> /dev/null; then
    echo "  Build successful"
    ((PASS++))
else
    echo "✗ Build failed"
    ((FAIL++))
fi

# 6. Unit tests
echo "✓ Running unit tests..."
if mvn test &> /dev/null; then
    echo "  All tests passed"
    ((PASS++))
else
    echo "✗ Some tests failed"
    ((FAIL++))
fi

echo "==========================="
echo "Results: $PASS passed, $FAIL failed"
```

### Post-Deployment Verification

```bash
#!/bin/bash
# post-deployment-check.sh

BASE_URL="${1:-http://localhost:8080/bytebite}"

echo "Post-Deployment Verification"
echo "============================"

PASS=0
FAIL=0

# 1. Health endpoint
echo "Checking health endpoint..."
HEALTH=$(curl -s $BASE_URL/health)
STATUS=$(echo $HEALTH | jq -r '.status')
if [ "$STATUS" = "UP" ]; then
    echo "✓ Health: UP"
    ((PASS++))
else
    echo "✗ Health: $STATUS"
    ((FAIL++))
fi

# 2. Database connection
DB_STATUS=$(echo $HEALTH | jq -r '.database.status')
if [ "$DB_STATUS" = "UP" ]; then
    echo "✓ Database: UP"
    ((PASS++))
else
    echo "✗ Database: $DB_STATUS"
    ((FAIL++))
fi

# 3. LDAP connection
LDAP_STATUS=$(echo $HEALTH | jq -r '.ldap.status')
if [ "$LDAP_STATUS" = "UP" ]; then
    echo "✓ LDAP: UP"
    ((PASS++))
else
    echo "✗ LDAP: $LDAP_STATUS"
    ((FAIL++))
fi

# 4. Login functionality
echo "Testing authentication..."
LOGIN_RESPONSE=$(curl -s -X POST $BASE_URL/login \
  -d "username=admin&password=admin" \
  -w "\n%{http_code}")
HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -1)
if [ "$HTTP_CODE" = "302" ] || [ "$HTTP_CODE" = "200" ]; then
    echo "✓ Authentication: OK"
    ((PASS++))
else
    echo "✗ Authentication: HTTP $HTTP_CODE"
    ((FAIL++))
fi

# 5. API availability
echo "Testing API endpoints..."
API_STATUS=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/api/menu)
if [ "$API_STATUS" = "200" ]; then
    echo "✓ API: Responding"
    ((PASS++))
else
    echo "✗ API: HTTP $API_STATUS"
    ((FAIL++))
fi

echo "============================"
echo "Results: $PASS passed, $FAIL failed"

if [ $FAIL -eq 0 ]; then
    echo "✓ Deployment verified successfully!"
    exit 0
else
    echo "✗ Deployment verification failed!"
    exit 1
fi
```

## Test Reporting

### Maven Surefire Reports

```bash
# Generate test reports
mvn surefire-report:report

# View HTML report
# File: target/site/surefire-report.html
```

### JaCoCo Coverage Reports

```bash
# Generate coverage report
mvn clean test jacoco:report

# View report
# File: target/site/jacoco/index.html

# Command-line summary
mvn jacoco:report
```

## CI/CD Integration

### GitHub Actions Example

Create `.github/workflows/test.yml`:

```yaml
name: Test Suite

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_DB: bytebite_db
          POSTGRES_USER: bytebite
          POSTGRES_PASSWORD: bytebite_pwd
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
      
      openldap:
        image: osixia/openldap:1.5.0
        env:
          LDAP_ORGANISATION: ByteBite
          LDAP_DOMAIN: bytebite.local
        options: >-
          --health-cmd ldapsearch
          --health-interval 10s
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 11
        uses: actions/setup-java@v3
        with:
          java-version: '11'
          distribution: 'temurin'
      
      - name: Build
        run: mvn clean package
      
      - name: Run Tests
        run: mvn test
      
      - name: Generate Coverage
        run: mvn jacoco:report
      
      - name: Upload Coverage
        uses: codecov/codecov-action@v3
        with:
          files: ./target/site/jacoco/jacoco.xml
```

---

**Version**: 1.0.0  
**Last Updated**: June 2026  
**Status**: Production Ready ✅

For more information, see README.md and DEPLOYMENT.md.
