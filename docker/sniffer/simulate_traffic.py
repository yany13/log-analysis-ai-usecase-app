#!/usr/bin/env python3
"""
ByteBite Network Sniffer - Traffic Simulation Tool
Injects realistic network packet logs for testing on Docker Desktop.

Usage:
    docker exec bytebite-sniffer python3 /usr/local/bin/simulate_traffic.py [scenario]

Scenarios:
    db_leak    - Database connection leak (rapid connection reuse)
    ldap_timeout - LDAP authentication timeout (30+ second wait)
    oom        - Out of memory (large unbounded query)
    deadlock   - Database deadlock (inverted lock ordering)
    ssl        - SSL certificate validation failure
    all        - Run all 5 scenarios (default)
"""

import sys
from datetime import datetime, timedelta
from pathlib import Path

class TrafficSimulator:
    def __init__(self, log_file="/logs/network_sniff.log"):
        self.log_file = log_file
        Path(log_file).parent.mkdir(parents=True, exist_ok=True)

    def append_log(self, entries):
        """Append log entries to file"""
        with open(self.log_file, 'a') as f:
            for entry in entries:
                f.write(entry + '\n')
        print(f"✓ Logged {len(entries)} entries to {self.log_file}")

    def simulate_db_leak(self):
        """Simulate DB connection leak scenario"""
        base_time = datetime.now()
        entries = []

        entries.append("")
        entries.append("# ========== SIMULATED: DB_LEAK Scenario ==========")
        entries.append("")

        t = base_time
        entries.append(f"[{t.strftime('%Y-%m-%d %H:%M:%S.000')}] Tomcat   172.18.0.2:45823 → 172.18.0.2:8080 [HTTP GET] \"/bytebite/api/menu?failureType=db_leak\" (98 bytes)")

        t += timedelta(milliseconds=100)
        entries.append(f"[{t.strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]}] PostgreSQL 172.18.0.2:52345 → 172.18.0.3:5432 [SQL QUERY] \"SELECT id, name FROM menu_items\" (512 bytes)")

        t += timedelta(milliseconds=67)
        entries.append(f"[{t.strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]}] PostgreSQL 172.18.0.2:52346 → 172.18.0.3:5432 [SQL QUERY] \"SELECT id, name FROM menu_items\" (512 bytes)")
        entries.append("  ⚠️  [MEDIUM] DB_LEAK: Rapid connection reuse (0.067s interval)")

        t += timedelta(milliseconds=89)
        entries.append(f"[{t.strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]}] PostgreSQL 172.18.0.2:52347 → 172.18.0.3:5432 [SQL QUERY] \"SELECT id, name FROM menu_items\" (512 bytes)")
        entries.append("  ⚠️  [MEDIUM] DB_LEAK: Rapid connection reuse (0.089s interval)")

        self.append_log(entries)

    def simulate_ldap_timeout(self):
        """Simulate LDAP timeout scenario"""
        base_time = datetime.now()
        entries = []

        entries.append("")
        entries.append("# ========== SIMULATED: LDAP_TIMEOUT Scenario ==========")
        entries.append("")

        t = base_time
        entries.append(f"[{t.strftime('%Y-%m-%d %H:%M:%S.000')}] Tomcat   172.18.0.2:45824 → 172.18.0.2:8080 [HTTP POST] \"/bytebite/login?failureType=ldap_timeout\" (156 bytes)")

        t += timedelta(milliseconds=150)
        entries.append(f"[{t.strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]}] LDAP     172.18.0.2:50235 → 172.18.0.4:389 [LDAP BIND] \"cn=admin,dc=bytebite,dc=local\" (89 bytes)")

        t += timedelta(seconds=30)
        entries.append(f"[{t.strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]}] LDAP     172.18.0.2:50235 → 172.18.0.4:389 [LDAP BIND] TIMEOUT (0 bytes)")
        entries.append("  ⚠️  [HIGH] LDAP_TIMEOUT: LDAP directory unavailable or timeout")

        self.append_log(entries)

    def simulate_oom(self):
        """Simulate Out-of-Memory scenario"""
        base_time = datetime.now()
        entries = []

        entries.append("")
        entries.append("# ========== SIMULATED: OOM Scenario ==========")
        entries.append("")

        t = base_time
        entries.append(f"[{t.strftime('%Y-%m-%d %H:%M:%S.000')}] Tomcat   172.18.0.2:45825 → 172.18.0.2:8080 [HTTP GET] \"/bytebite/api/analytics?action=all-time-report&failureType=oom\" (189 bytes)")

        t += timedelta(milliseconds=120)
        entries.append(f"[{t.strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]}] PostgreSQL 172.18.0.2:52348 → 172.18.0.3:5432 [SQL QUERY] \"SELECT * FROM analytics_events\" (187432 bytes)")
        entries.append("  ⚠️  [HIGH] OOM: Large result set detected (OOM risk) (187432 bytes)")

        self.append_log(entries)

    def simulate_deadlock(self):
        """Simulate Deadlock scenario"""
        base_time = datetime.now()
        entries = []

        entries.append("")
        entries.append("# ========== SIMULATED: DEADLOCK Scenario ==========")
        entries.append("")

        t = base_time
        entries.append(f"[{t.strftime('%Y-%m-%d %H:%M:%S.000')}] Tomcat   172.18.0.2:45826 → 172.18.0.2:8080 [HTTP POST] \"/bytebite/api/kds?failureType=deadlock\" (289 bytes)")

        t += timedelta(milliseconds=150)
        entries.append(f"[{t.strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]}] PostgreSQL 172.18.0.2:52349 → 172.18.0.3:5432 [SQL UPDATE] \"UPDATE orders SET status='IN_PROGRESS'\" (1024 bytes)")

        t += timedelta(milliseconds=200)
        entries.append(f"[{t.strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]}] PostgreSQL 172.18.0.2:52350 → 172.18.0.3:5432 [SQL UPDATE] \"UPDATE inventory SET qty=qty-1\" (896 bytes)")

        t += timedelta(milliseconds=450)
        entries.append(f"[{t.strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]}] PostgreSQL 172.18.0.2:52349 → 172.18.0.3:5432 [SQL QUERY] \"RETRY: deadlocked transaction\" (2048 bytes)")
        entries.append("  ⚠️  [HIGH] DEADLOCK: SQL deadlock (inverted lock ordering)")

        self.append_log(entries)

    def simulate_ssl(self):
        """Simulate SSL failure scenario"""
        base_time = datetime.now()
        entries = []

        entries.append("")
        entries.append("# ========== SIMULATED: SSL Scenario ==========")
        entries.append("")

        t = base_time
        entries.append(f"[{t.strftime('%Y-%m-%d %H:%M:%S.000')}] Tomcat   172.18.0.2:45827 → 172.18.0.2:8443 [HTTP POST] \"/bytebite/checkout?failureType=ssl\" (278 bytes)")

        t += timedelta(milliseconds=50)
        entries.append(f"[{t.strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]}] Tomcat   172.18.0.2:45827 → 172.18.0.2:8443 [TLS HANDSHAKE] \"ClientHello\" (256 bytes)")

        t += timedelta(milliseconds=40)
        entries.append(f"[{t.strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]}] Tomcat   172.18.0.2:45827 → 172.18.0.2:8443 [TLS HANDSHAKE] \"cert=self-signed,CN=localhost\" (512 bytes)")

        t += timedelta(milliseconds=30)
        entries.append(f"[{t.strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]}] Tomcat   172.18.0.2:45827 → 172.18.0.2:8443 [CONNECTION RESET] \"Certificate validation failed\" (0 bytes)")
        entries.append("  ⚠️  [HIGH] SSL: TLS/SSL certificate validation failure")

        self.append_log(entries)

    def run(self, scenario="all"):
        """Run simulation for specified scenario(s)"""
        if scenario == "all":
            print("Running all 5 failure scenarios...")
            self.simulate_db_leak()
            self.simulate_ldap_timeout()
            self.simulate_oom()
            self.simulate_deadlock()
            self.simulate_ssl()
        elif scenario == "db_leak":
            self.simulate_db_leak()
        elif scenario == "ldap_timeout":
            self.simulate_ldap_timeout()
        elif scenario == "oom":
            self.simulate_oom()
        elif scenario == "deadlock":
            self.simulate_deadlock()
        elif scenario == "ssl":
            self.simulate_ssl()
        else:
            print(f"Unknown scenario: {scenario}")
            sys.exit(1)

        print(f"\n✓ Simulation complete! View logs:")
        print(f"   docker exec bytebite-sniffer tail -100 /logs/network_sniff.log")


if __name__ == "__main__":
    scenario = sys.argv[1] if len(sys.argv) > 1 else "all"
    simulator = TrafficSimulator()
    simulator.run(scenario)
