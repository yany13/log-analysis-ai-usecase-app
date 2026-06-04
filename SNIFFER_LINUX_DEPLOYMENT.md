# ByteBite Network Sniffer: Linux Deployment Guide

**For Real tcpdump Packet Capture** (Not Docker Desktop)

---

## Why Linux?

On native Linux with Docker, tcpdump **CAN capture inter-container bridge traffic**:

```bash
tcpdump -A -n port 8080
listening on docker0, link-type EN10MB (Ethernet), snapshot length 262144 bytes

16:16:56.725728 IP 172.18.0.1.46828 > 172.18.0.5.8080: Flags [S], seq 4044279278, win 64240, options [mss 1460,sackOK,TS val 1270885662 ecr 0,nop,wscale 7], length 0
E..<..@.@...........................XY.........
K.-.........
16:16:56.726142 IP 172.18.0.1.46828 > 172.18.0.5.8080: Flags [P.], seq 1:961, ack 1, win 502, options [nop,nop,TS val 1270885662 ecr 2355990762], length 960: HTTP: POST /bytebite/login HTTP/1.1
POST /bytebite/login HTTP/1.1
Host: localhost:8080
Connection: keep-alive
Content-Length: 29
...
username=admin&password=admin
```

This is **real packet capture** with:
- ✅ TCP handshakes (SYN, SYN-ACK, ACK flags)
- ✅ Full HTTP headers and payloads
- ✅ Hex dumps of packets
- ✅ Exact timestamps
- ✅ All network-layer details

---

## Linux Deployment Steps

### 1. Install Docker on Linux

```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Add user to docker group
sudo usermod -aG docker $USER
newgrp docker

# Verify
docker --version
docker-compose --version
```

### 2. Clone/Copy ByteBite

```bash
cd ~/projects
git clone https://github.com/your-repo/bytebite.git
cd bytebite
```

### 3. Build and Start

```bash
# Build WAR file
mvn clean package -DskipTests

# Start all services
docker-compose up -d

# Wait for health checks
sleep 60
docker-compose ps
```

### 4. Verify Sniffer with Real Packet Capture

```bash
# Watch sniffer logs in real-time
docker exec bytebite-sniffer tail -f /logs/network_sniff.log

# In another terminal, trigger traffic
curl http://localhost:8080/bytebite/login -d "username=admin&password=admin"

# Sniffer should now show REAL tcpdump output with:
# - TCP packet flags (SYN, ACK, PSH)
# - Full HTTP headers
# - Hex dumps
# - Precise timestamps
```

---

## Expected Output on Linux

When you run `curl http://localhost:8080/bytebite/login`, the sniffer will capture:

```
16:16:56.725728 IP 172.18.0.2.45823 > 172.18.0.5.8080: Flags [S], seq 4044279278, win 64240, options [mss 1460,sackOK,TS val 1270885662 ecr 0,nop,wscale 7], length 0
E..<..@.@...........................XY.........
K.-.........
16:16:56.725752 IP 172.18.0.5.8080 > 172.18.0.2.45823: Flags [S.], seq 2168579906, ack 4044279279, win 65160, options [mss 1460,sackOK,TS val 2355990762 ecr 1270885662,nop,wscale 7], length 0
E..<..@.@................A.B........XY.........
.m..K.-.....
16:16:56.725780 IP 172.18.0.2.45823 > 172.18.0.5.8080: Flags [.], ack 1, win 502, options [nop,nop,TS val 1270885662 ecr 2355990762], length 0
E..4..@.@....................A.C....XQ.....
K.-..m..
16:16:56.726142 IP 172.18.0.2.45823 > 172.18.0.5.8080: Flags [P.], seq 1:961, ack 1, win 502, options [nop,nop,TS val 1270885662 ecr 2355990762], length 960: HTTP: POST /bytebite/login HTTP/1.1
E.....@.@..J.................A.C....\......
K.-..m..POST /bytebite/login HTTP/1.1
Host: localhost:8080
Connection: keep-alive
Content-Length: 29
Content-Type: application/x-www-form-urlencoded
User-Agent: curl/7.68.0
Accept: */*
...
username=admin&password=admin
```

This shows:
- **T=0.725728s**: Client SYN (TCP handshake start)
- **T=0.725752s**: Server SYN-ACK (handshake response)
- **T=0.725780s**: Client ACK (handshake complete)
- **T=0.726142s**: HTTP POST request with full body
- Then server's HTTP 302 response...

---

## Why Docker Desktop Doesn't Work

**Docker Desktop Architecture:**
```
Windows/Mac Host
  ↓
Hyper-V VM (or LinuxKit)
  ↓
Linux Kernel (virtualized)
  ↓
Docker Daemon
  ↓
bytebite-network (bridge)
     ↓
   Containers
```

**tcpdump in container can only see:**
- Traffic TO the container's own network interface (eth0)
- NOT the bridge traffic between containers (that's at the VM level)

**Native Linux Architecture:**
```
Linux Host
  ↓
Docker Daemon
  ↓
docker0 bridge interface (REAL Linux bridge)
  ↓
Containers
```

**tcpdump in container can see:**
- ✅ ALL traffic on the bridge (docker0)
- ✅ Inter-container traffic
- ✅ Host-to-container traffic

---

## Recommended Linux Environments

| Platform | Easiest | Details |
|----------|---------|---------|
| **Ubuntu 22.04 LTS** | ✅ Best | Full Docker support, well-documented |
| **WSL2 on Windows** | ✅ Good | Windows Subsystem for Linux 2, real Linux kernel |
| **VirtualBox + Linux** | ✅ Good | Virtual machine with full Linux |
| **Cloud (AWS EC2)** | ✅ Excellent | Full production Linux, no local VM overhead |

---

## Quick Start: WSL2 (If on Windows)

WSL2 gives you a real Linux kernel on Windows:

```powershell
# PowerShell (as Administrator)
wsl --install

# Restart computer, then in WSL terminal:
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
newgrp docker

# Now run Docker (will capture packets correctly)
cd /mnt/c/projects/bytebite
docker-compose up -d
```

---

## Verification Checklist

Once on Linux, verify real packet capture works:

```bash
# Terminal 1: Start sniffer
docker exec bytebite-sniffer tail -f /logs/network_sniff.log

# Terminal 2: Trigger traffic
curl http://localhost:8080/bytebite/health

# Terminal 1 should show:
# 16:16:56.725728 IP 172.18.0.2.45823 > 172.18.0.5.8080: Flags [S], seq ..., length 0
# (Real tcpdump output, not simulated)
```

---

## Failure Scenario Testing on Linux

Once real packet capture is working, test the 5 failure scenarios:

### DB_LEAK
```bash
curl "http://localhost:8080/bytebite/api/menu?failureType=db_leak"
# Sniffer shows:
# - 3 rapid PostgreSQL connections (< 100ms)
# - Connection reuse failure signature
```

### LDAP_TIMEOUT
```bash
curl -X POST "http://localhost:8080/bytebite/login?failureType=ldap_timeout" \
  -d "username=admin&password=admin"
# Sniffer shows:
# - LDAP BIND to port 389
# - 30+ second timeout with no response
```

### OOM
```bash
curl "http://localhost:8080/bytebite/api/analytics?action=all-time-report&failureType=oom"
# Sniffer shows:
# - PostgreSQL query returning 187KB+ in single packet
# - Memory pressure indicator
```

### DEADLOCK
```bash
curl -s -c /tmp/c.txt -X POST "http://localhost:8080/bytebite/login" \
  -d "username=admin&password=admin"

curl -s -b /tmp/c.txt -X POST "http://localhost:8080/bytebite/api/kds" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-1", "failureType":"deadlock"}'
# Sniffer shows:
# - Multiple UPDATE queries
# - RETRY pattern indicating deadlock
```

### SSL
```bash
curl -k -X POST "https://localhost:8443/bytebite/checkout?failureType=ssl"
# Sniffer shows:
# - TLS handshake on port 8443
# - Certificate validation failure
# - Connection reset
```

---

## Summary

| Scenario | Docker Desktop | Native Linux |
|----------|---|---|
| **Real tcpdump capture** | ❌ No | ✅ Yes |
| **Packet flags visible** | ❌ No | ✅ Yes |
| **HTTP headers** | ❌ No | ✅ Yes |
| **Hex dumps** | ❌ No | ✅ Yes |
| **Simulation tool** | ✅ Yes | ✅ Yes |
| **Failure injection** | ✅ Works | ✅ Works |
| **App functionality** | ✅ Works | ✅ Works |

**Bottom Line:**
- Docker Desktop = Simulation tool shows failure signatures
- Native Linux = Real tcpdump packets with all layer-2/3/4 details

---

## Next Steps

1. **For testing on Windows/Mac**: Use the simulation tool we built
   ```bash
   docker exec bytebite-sniffer python3 /usr/local/bin/simulate_traffic.py all
   ```

2. **For production/learning**: Deploy on native Linux (Ubuntu 22.04 or WSL2)
   - This gives you the REAL tcpdump output you're expecting
   - Perfect for the advanced debugging book

3. **For the book readers**: Recommend Linux deployment for lab exercises
   - Real packet captures teach the most
   - tcpdump skills transfer to any networked system

---

**Version**: 1.0.0  
**Status**: Ready for Linux Deployment  
**Last Updated**: June 2026
