#!/usr/bin/env python3
"""
ByteBite Network Sniffer - Alternative Implementation
Uses scapy for packet capture (better Docker Desktop compatibility)
"""

import sys
import json
from datetime import datetime
from pathlib import Path
import socket
import threading
import time

# Try to import scapy
try:
    from scapy.all import sniff, IP, TCP, UDP, Raw
    SCAPY_AVAILABLE = True
except ImportError:
    SCAPY_AVAILABLE = False
    print("[WARNING] scapy not available, will use socket-based capture", file=sys.stderr)

class NetworkSniffer:
    def __init__(self, log_file, ports=[8080, 8443, 5432, 389]):
        self.log_file = log_file
        self.ports = ports
        Path(log_file).parent.mkdir(parents=True, exist_ok=True)
        self.log_enabled = True

    def packet_callback(self, packet):
        """Process captured packet"""
        try:
            if not packet.haslayer(IP):
                return

            ip_layer = packet[IP]
            src_ip = ip_layer.src
            dst_ip = ip_layer.dst

            if not packet.haslayer(TCP) and not packet.haslayer(UDP):
                return

            # Get ports
            if packet.haslayer(TCP):
                proto = "TCP"
                src_port = packet[TCP].sport
                dst_port = packet[TCP].dport
            else:
                proto = "UDP"
                src_port = packet[UDP].sport
                dst_port = packet[UDP].dport

            # Check if ports match our filter
            if src_port not in self.ports and dst_port not in self.ports:
                return

            # Extract payload
            payload = ""
            if packet.haslayer(Raw):
                payload_bytes = packet[Raw].load[:100]  # First 100 bytes
                try:
                    payload = payload_bytes.decode('utf-8', errors='ignore').replace('\n', ' ')[:80]
                except:
                    payload = str(payload_bytes)[:80]

            # Determine protocol based on port
            service = self.get_service(src_port, dst_port)

            # Format log entry
            timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]
            log_entry = f"[{timestamp}] {service:12} {src_ip:15}:{src_port:5} → {dst_ip:15}:{dst_port:5} {proto:3}"

            if payload:
                log_entry += f' "{payload}"'

            # Write to log file
            self.write_log(log_entry)

            # Print to console
            print(log_entry)
            sys.stdout.flush()

        except Exception as e:
            print(f"[ERROR] {str(e)}", file=sys.stderr)

    def get_service(self, src_port, dst_port):
        """Determine service name from ports"""
        port_map = {
            8080: "HTTP",
            8443: "HTTPS",
            5432: "PostgreSQL",
            389: "LDAP"
        }

        if src_port in port_map:
            return port_map[src_port]
        elif dst_port in port_map:
            return port_map[dst_port]
        return "UNKNOWN"

    def write_log(self, entry):
        """Write to log file"""
        try:
            with open(self.log_file, 'a') as f:
                f.write(entry + '\n')
        except Exception as e:
            print(f"[ERROR] Failed to write log: {e}", file=sys.stderr)

    def start_scapy_capture(self):
        """Start packet capture with scapy"""
        print(f"[{datetime.now()}] Starting packet capture with scapy...", file=sys.stderr)
        try:
            # BPF filter for ports
            bpf_filter = f"port {' or port '.join(map(str, self.ports))}"
            print(f"[{datetime.now()}] BPF Filter: {bpf_filter}", file=sys.stderr)

            sniff(
                prn=self.packet_callback,
                filter=bpf_filter,
                store=False,
                iface=None  # Sniff on all interfaces
            )
        except Exception as e:
            print(f"[ERROR] Scapy capture failed: {e}", file=sys.stderr)
            self.start_socket_fallback()

    def start_socket_fallback(self):
        """Fallback: Listen on ports directly with sockets"""
        print(f"[{datetime.now()}] Starting fallback socket-based listener...", file=sys.stderr)

        for port in self.ports:
            threading.Thread(target=self.listen_on_port, args=(port,), daemon=True).start()

        # Keep running
        try:
            while True:
                time.sleep(1)
        except KeyboardInterrupt:
            pass

    def listen_on_port(self, port):
        """Listen on a port and log connections"""
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

            # Listen on localhost for traffic originating from docker network
            sock.bind(('127.0.0.1', port))
            sock.listen(1)

            print(f"[{datetime.now()}] Listening on port {port}...", file=sys.stderr)

            while True:
                try:
                    conn, addr = sock.accept()
                    timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]
                    service = self.get_service(addr[1], port)
                    log_entry = f"[{timestamp}] {service:12} {addr[0]:15}:{addr[1]:5} → 127.0.0.1:{port:5} TCP (connection)"
                    self.write_log(log_entry)
                    print(log_entry)
                    conn.close()
                except:
                    pass
        except Exception as e:
            print(f"[ERROR] Failed to listen on port {port}: {e}", file=sys.stderr)

    def run(self):
        """Start the sniffer"""
        print(f"[{datetime.now()}] ByteBite Network Sniffer Started", file=sys.stderr)
        print(f"[{datetime.now()}] Ports: {self.ports}", file=sys.stderr)
        print(f"[{datetime.now()}] Log file: {self.log_file}", file=sys.stderr)

        if SCAPY_AVAILABLE:
            print(f"[{datetime.now()}] Using scapy for packet capture", file=sys.stderr)
            self.start_scapy_capture()
        else:
            print(f"[{datetime.now()}] scapy not available, using fallback socket listener", file=sys.stderr)
            self.start_socket_fallback()

if __name__ == '__main__':
    log_file = sys.argv[1] if len(sys.argv) > 1 else '/logs/network_sniff.log'
    sniffer = NetworkSniffer(log_file)
    sniffer.run()
