@echo off
setlocal enabledelayedexpansion

REM Create cookie file location
if not exist C:\temp mkdir C:\temp
set COOKIES=C:\temp\cookies.txt

REM Step 1: Login
echo === Logging in ===
curl -X POST "http://localhost:8080/bytebite/login" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"admin\"}" ^
  -c %COOKIES%
echo.

REM ========================================
REM 1. DB LEAK
REM ========================================
echo === 1. DB LEAK ===
curl -X POST "http://localhost:8080/bytebite/api/menu" ^
  -H "Content-Type: application/json" ^
  -d "{\"failureType\":\"db_leak\"}" ^
  -b %COOKIES%
echo.

REM ========================================
REM 2. LDAP TIMEOUT
REM ========================================
echo === 2. LDAP TIMEOUT ===
curl -X POST "http://localhost:8080/bytebite/login" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"admin\",\"failureType\":\"ldap_timeout\"}"
echo.

REM ========================================
REM 3. OOM
REM ========================================
echo === 3. OOM ===
curl -X POST "http://localhost:8080/bytebite/api/analytics" ^
  -H "Content-Type: application/json" ^
  -d "{\"action\":\"all-time-report\",\"failureType\":\"oom\"}" ^
  -b %COOKIES%
echo.

REM ========================================
REM 4. DEADLOCK
REM ========================================
echo === 4. DEADLOCK ===
curl -X POST "http://localhost:8080/bytebite/api/kds" ^
  -H "Content-Type: application/json" ^
  -d "{\"orderId\":\"ORD-001\",\"status\":\"IN_PROGRESS\",\"failureType\":\"deadlock\"}" ^
  -b %COOKIES%
echo.

REM ========================================
REM 5. SSL
REM ========================================
echo === 5. SSL ===
curl -X POST "http://localhost:8080/bytebite/checkout" ^
  -H "Content-Type: application/json" ^
  -d "{\"totalAmount\":100,\"failureType\":\"ssl\"}" ^
  -b %COOKIES%

echo.
echo === Test Complete ===
pause