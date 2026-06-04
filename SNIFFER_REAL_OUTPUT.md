# ByteBite Network Sniffer - Real tcpdump Output on Docker Desktop

## ✅ Verified Working: Real tcpdump Packet Capture

This document shows **actual tcpdump output** captured on Docker Desktop with the ByteBite network sniffer.

---

## Real Captured Output

### HTTP GET /bytebite/health Request:

```
16:25:53.314847 IP6 ::1.40168 > ::1.8080: Flags [S], seq 1361285855, win 65476, options [mss 65476,sackOK,TS val 2081203892 ecr 0,nop,wscale 7], length 0
`....(.@....................................Q#...........0.........
|...........

16:25:53.314868 IP6 ::1.8080 > ::1.40168: Flags [S.], seq 3306541376, ack 1361285856, win 65464, options [mss 65476,sackOK,TS val 2081203892 ecr 2081203892,nop,wscale 7], length 0
`	...(.@.......................................@Q#.......0.........
|...|.......

16:25:53.314884 IP6 ::1.40168 > ::1.8080: Flags [.], ack 1, win 512, options [nop,nop,TS val 2081203892 ecr 2081203892], length 0
`.... .@....................................Q#.....A.....(.....
|...|...

16:25:53.314998 IP6 ::1.40168 > ::1.8080: Flags [P.], seq 1:93, ack 1, win 512, options [nop,nop,TS val 2081203892 ecr 2081203892], length 92: HTTP: GET /bytebite/health HTTP/1.1
`....|.@....................................Q#.....A...........
|...|...
GET /bytebite/health HTTP/1.1
Host: localhost:8080
User-Agent: curl/8.5.0
Accept: */*

16:25:53.315004 IP6 ::1.8080 > ::1.40168: Flags [.], ack 93, win 511, options [nop,nop,TS val 2081203892 ecr 2081203892], length 0
`	... .@.......................................AQ#.<.....(.....
|...|...

16:25:53.318790 IP6 ::1.8080 > ::1.40168: Flags [P.], seq 1:1034, ack 93, win 512, options [nop,nop,TS val 2081203896 ecr 2081203892], length 1033: HTTP: HTTP/1.1 200 
`	...).@.......................................AQ#.<.....1.....
|...|...

HTTP/1.1 200 
Cache-Control: private
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: geolocation=(), microphone=(), camera=()
Server: ByteBite
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS
Access-Control-Allow-Headers: Content-Type,Authorization
Access-Control-Expose-Headers: Authorization
Access-Control-Max-Age: 3600
Access-Control-Allow-Credentials: true
Content-Type: application/json;charset=UTF-8
Content-Length: 217
Date: Thu, 04 Jun 2026 16:25:53 GMT

{"database":{"message":"Database connection successful","status":"UP"},"ldap":{"message":"LDAP configured at openldap:389","status":"UP"},"service":"ByteBite","version":"1.0.0","status":"UP","timestamp":1780590353316}

16:25:53.318826 IP6 ::1.40168 > ::1.8080: Flags [.], ack 1034, win 540, options [nop,nop,TS val 2081203896 ecr 2081203896], length 0
`.... .@....................................Q#.<...J.....(.....
|...|...

16:25:53.319038 IP6 ::1.40168 > ::1.8080: Flags [F.], seq 93, ack 1034, win 540, options [nop,nop,TS val 2081203896 ecr 2081203896], length 0
`.... .@....................................Q#.<...J.....(.....
|...|...

16:25:53.319297 IP6 ::1.8080 > ::1.40168: Flags [F.], seq 1034, ack 94, win 512, options [nop,nop,TS val 2081203896 ecr 2081203896], length 0
`	... .@.......................................JQ#.=.....(.....
|...|...

16:25:53.319316 IP6 ::1.40168 > ::1.8080: Flags [.], ack 1035, win 540, options [nop,nop,TS val 2081203896 ecr 2081203896], length 0
`.... .@....................................Q#.=...K.....(.....
|...|...
```

---

## What This Shows

### ✅ TCP Handshake (3-way handshake)

```
T=0.314847s: Flags [S]   - SYN from client (::1.40168)
T=0.314868s: Flags [S.]  - SYN-ACK from server (::1.8080)
T=0.314884s: Flags [.]   - ACK from client
```

### ✅ HTTP Request with Headers

```
T=0.314998s: Flags [P.], length 92: GET /bytebite/health HTTP/1.1
GET /bytebite/health HTTP/1.1
Host: localhost:8080
User-Agent: curl/8.5.0
Accept: */*
```

### ✅ HTTP Response with Headers and Body

```
T=0.318790s: Flags [P.], length 1033: HTTP/1.1 200 
HTTP/1.1 200 
Cache-Control: private
X-Content-Type-Options: nosniff
...
Content-Type: application/json;charset=UTF-8
Content-Length: 217
Date: Thu, 04 Jun 2026 16:25:53 GMT

{"database":{"message":"Database connection successful","status":"UP"},...}
```

### ✅ Connection Teardown (FIN handshake)

```
T=0.319038s: Flags [F.] - FIN from client
T=0.319297s: Flags [F.] - FIN from server
T=0.319316s: Flags [.] - ACK from client
```

---

## How to Capture This on Docker Desktop

### Step 1: Run tcpdump on the Tomcat container

```bash
docker exec bytebite-tomcat timeout 10 tcpdump -A -n -i lo port 8080
```

**Key parameters:**
- `-A`: Print ASCII data (shows HTTP headers and body)
- `-n`: Don't resolve addresses (show IPs, not hostnames)
- `-i lo`: Listen on loopback interface (where localhost:8080 traffic flows)
- `port 8080`: Filter for Tomcat port only
- `timeout 10`: Stop tcpdump after 10 seconds

### Step 2: Trigger Traffic (from INSIDE a container)

```bash
# Make requests from inside a container (not from host)
docker exec bytebite-tomcat curl http://localhost:8080/bytebite/health
docker exec bytebite-tomcat curl http://localhost:8080/bytebite/api/menu
```

### Step 3: tcpdump Captures Both Request and Response

All traffic is visible in the tcpdump output:
- Complete TCP handshake
- Full HTTP request headers
- Full HTTP response headers and body
- Connection teardown

---

## Summary: Docker Desktop vs Native Linux

| Feature | Docker Desktop | Native Linux |
|---------|---|---|
| **loopback (lo) capture** | ✅ **WORKS** | ✅ Works |
| **HTTP headers visible** | ✅ **YES** | ✅ YES |
| **HTTP body visible** | ✅ **YES** | ✅ YES |
| **Inter-container traffic (eth0)** | ❌ No (can't see) | ✅ YES |
| **Host-to-container traffic** | ⚠️ Limited | ✅ YES |

---

## Key Insight

**Docker Desktop DOES support tcpdump**, but:
- ✅ Works on **loopback (lo)** - traffic to/from localhost inside containers
- ❌ Doesn't work on **eth0/any** - inter-container bridge traffic (virtualization boundary)

**Solution**: Make requests from **within containers** to **localhost**, and tcpdump captures everything perfectly!

---

## Real Production Use

```bash
# Start tcpdump capture in background, log to file
docker exec bytebite-tomcat sh -c "tcpdump -A -n -i lo port 8080 > /tmp/network.cap 2>&1" &

# Make requests from inside container
docker exec bytebite-tomcat curl http://localhost:8080/bytebite/login -d "user=admin&pass=admin"
docker exec bytebite-tomcat curl http://localhost:8080/bytebite/api/menu
docker exec bytebite-tomcat curl http://localhost:8080/bytebite/health

# Stop capture
pkill tcpdump

# View captured packets
docker exec bytebite-tomcat cat /tmp/network.cap
```

---

## Version
- **Tested On**: Docker Desktop (Windows)
- **Date**: 2026-06-04
- **Status**: ✅ Verified Working
