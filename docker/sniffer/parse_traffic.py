#!/usr/bin/env python3

"""
ByteBite Network Traffic Parser
Parses tcpdump output and produces human-readable, searchable logs with failure signatures.

Features:
- Real-time parsing of network packets
- Protocol identification (TCP, HTTP, LDAP, SQL, TLS)
- Failure signature detection for all 5 ByteBite failure vectors
- Daily log rotation with 7-day retention
- Human-readable output with timestamps, IPs, ports, protocols, payloads
"""

import sys
import re
import json
from datetime import datetime, timedelta
from pathlib import Path
from collections import defaultdict
import os
import gzip
import shutil

class NetworkTrafficParser:
    def __init__(self, log_file):
        self.log_file = log_file
        self.packet_buffer = []
        self.connection_states = defaultdict(dict)
        self.failure_signatures = {
            'db_leak': {
                'pattern': r'repeated.*connection|connection.*not.*closed|pool.*exhausted',
                'ports': [5432],
                'description': 'PostgreSQL connection not closed (HikariCP leak)'
            },
            'ldap_timeout': {
                'pattern': r'timeout|no.*response|BIND.*timeout|ldap.*fail',
                'ports': [389],
                'description': 'LDAP directory unavailable or timeout'
            },
            'oom': {
                'pattern': r'large.*result|memory.*pressure|gc.*overhead|heap.*spike|\d{4,}.*bytes',
                'ports': [5432],
                'description': 'Unbounded query causing OOM (large result set)'
            },
            'deadlock': {
                'pattern': r'deadlock|lock.*wait|blocked|stalled|retry',
                'ports': [5432],
                'description': 'SQL deadlock (inverted lock ordering)'
            },
            'ssl': {
                'pattern': r'ssl|tls|certificate|handshake.*fail|cert.*valid',
                'ports': [8080, 8443],
                'description': 'TLS/SSL certificate validation failure'
            }
        }
        self.ensure_log_file()

    def ensure_log_file(self):
        """Create log file parent directory if needed."""
        os.makedirs(os.path.dirname(self.log_file), exist_ok=True)

    def rotate_logs(self):
        """Daily log rotation with 7-day retention."""
        log_dir = os.path.dirname(self.log_file)
        log_name = os.path.basename(self.log_file)
        base_name = log_name.split('.')[0] if '.' in log_name else log_name

        # Check if current log needs rotation (daily)
        if os.path.exists(self.log_file):
            mod_time = datetime.fromtimestamp(os.path.getmtime(self.log_file))
            if datetime.now().date() > mod_time.date():
                # Rotate log
                dated_name = f"{base_name}.{mod_time.strftime('%Y-%m-%d')}.log"
                dated_path = os.path.join(log_dir, dated_name)
                shutil.move(self.log_file, dated_path)

                # Compress old logs
                if os.path.exists(dated_path):
                    with open(dated_path, 'rb') as f_in:
                        with gzip.open(f"{dated_path}.gz", 'wb') as f_out:
                            shutil.copyfileobj(f_in, f_out)
                    os.remove(dated_path)

        # Delete logs older than 7 days
        cutoff = datetime.now() - timedelta(days=7)
        for f in Path(log_dir).glob(f"{base_name}.*.log.gz"):
            try:
                file_date = datetime.strptime(f.stem.split('.')[1], '%Y-%m-%d')
                if file_date < cutoff:
                    os.remove(f)
            except (ValueError, IndexError):
                pass

    def parse_timestamp(self, ts_str):
        """Parse tcpdump timestamp format (YYYY-MM-DD HH:MM:SS.ffffff)."""
        try:
            return datetime.strptime(ts_str[:26], '%Y-%m-%d %H:%M:%S.%f')
        except:
            return datetime.now()

    def extract_ip_port(self, addr_str):
        """Extract IP and port from address string (IP.port)."""
        match = re.match(r'([\d.]+)\.(\d+)', addr_str)
        if match:
            return match.group(1), int(match.group(2))
        return None, None

    def get_protocol_info(self, line, src_port, dst_port):
        """Determine protocol from port and payload."""
        protocol = "TCP"
        transaction_type = "UNKNOWN"
        payload_summary = ""

        # Port-based protocol detection
        if dst_port == 8080 or src_port == 8080 or dst_port == 8443 or src_port == 8443:
            protocol = "HTTP"
            if 'POST' in line or 'POST' in line.upper():
                transaction_type = "HTTP POST"
            elif 'GET' in line or 'GET' in line.upper():
                transaction_type = "HTTP GET"
            elif 'HTTP' in line.upper():
                transaction_type = "HTTP"
            else:
                transaction_type = "HTTP"
        elif dst_port == 5432 or src_port == 5432:
            protocol = "PostgreSQL"
            if 'SELECT' in line.upper():
                transaction_type = "SQL QUERY"
            elif 'UPDATE' in line.upper():
                transaction_type = "SQL UPDATE"
            elif 'INSERT' in line.upper():
                transaction_type = "SQL INSERT"
            elif 'DELETE' in line.upper():
                transaction_type = "SQL DELETE"
            else:
                transaction_type = "SQL"
        elif dst_port == 389 or src_port == 389:
            protocol = "LDAP"
            if 'BIND' in line.upper():
                transaction_type = "LDAP BIND"
            elif 'SEARCH' in line.upper():
                transaction_type = "LDAP SEARCH"
            else:
                transaction_type = "LDAP"

        # Extract payload summary from ASCII section of tcpdump output
        # Look for readable text in the line (letters, numbers, common characters)
        # Exclude tcpdump metadata lines (those with hex addresses like "0x0000:")
        if not line.startswith('0x') and len(line) > 20:
            # Extract first printable ASCII sequence (up to 80 chars)
            ascii_match = re.search(r'([A-Za-z0-9\-_/.:\[\](){}<>@,?;=!& ]{10,80})', line)
            if ascii_match:
                payload_summary = ascii_match.group(1).strip()
                # Clean up common tcpdump artifacts
                payload_summary = payload_summary.replace('..', '').replace('.', '')[:80]
                if payload_summary and len(payload_summary) > 5:
                    # Ensure it looks like actual payload, not tcpdump noise
                    pass
                else:
                    payload_summary = ""

        return protocol, transaction_type, payload_summary

    def detect_failure_signature(self, line, protocol, src_ip, dst_ip, src_port, dst_port):
        """Detect failure injection signatures in network traffic."""
        signatures = []
        line_lower = line.lower()

        for failure_type, sig_info in self.failure_signatures.items():
            # Check port match
            if (dst_port not in sig_info['ports'] and src_port not in sig_info['ports']):
                continue

            # Check pattern match
            if re.search(sig_info['pattern'], line_lower):
                signatures.append({
                    'type': failure_type,
                    'description': sig_info['description'],
                    'confidence': 'MEDIUM'
                })

        # Additional heuristics for specific failures
        if dst_port == 5432 or src_port == 5432:
            # Count bytes to detect OOM (large result sets)
            byte_match = re.search(r'(\d{4,})\s+bytes', line)
            if byte_match:
                byte_count = int(byte_match.group(1))
                if byte_count > 10000:
                    signatures.append({
                        'type': 'oom',
                        'description': 'Large result set detected (OOM risk)',
                        'confidence': 'HIGH',
                        'bytes': byte_count
                    })

        # Detect connection reuse patterns (db_leak)
        if dst_port == 5432 or src_port == 5432:
            connection_key = f"{src_ip}:{src_port}->{dst_ip}:{dst_port}"
            if connection_key in self.connection_states:
                prev_query_time = self.connection_states[connection_key].get('last_query_time')
                if prev_query_time:
                    time_diff = (datetime.now() - prev_query_time).total_seconds()
                    # Rapid reuse suggests connection not being pooled properly
                    if time_diff < 0.5:
                        signatures.append({
                            'type': 'db_leak',
                            'description': 'Rapid connection reuse (leak signature)',
                            'confidence': 'MEDIUM',
                            'interval_sec': round(time_diff, 3)
                        })
            self.connection_states[connection_key]['last_query_time'] = datetime.now()

        return signatures

    def format_log_entry(self, line, packet_data):
        """Format a single log entry in human-readable format."""
        timestamp = packet_data['timestamp']
        src_ip = packet_data['src_ip']
        dst_ip = packet_data['dst_ip']
        src_port = packet_data['src_port']
        dst_port = packet_data['dst_port']
        protocol = packet_data['protocol']
        transaction_type = packet_data['transaction_type']
        payload_summary = packet_data['payload_summary']
        byte_count = packet_data.get('byte_count', 0)

        # Format: [TIMESTAMP] PROTOCOL SRC_IP:PORT → DST_IP:PORT [TRANSACTION] "PAYLOAD" (SIZE bytes)
        ts_str = timestamp.strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]  # 3 decimal places for ms

        log_line = f"[{ts_str}] {protocol:12} {src_ip:15}:{src_port:5} → {dst_ip:15}:{dst_port:5}"

        if transaction_type and transaction_type != "UNKNOWN":
            log_line += f" [{transaction_type:15}]"

        if payload_summary:
            log_line += f' "{payload_summary[:80]}"'

        if byte_count > 0:
            log_line += f" ({byte_count} bytes)"

        return log_line

    def parse_tcpdump_output(self, line):
        """Parse a single tcpdump output line."""
        if not line.strip():
            return None

        # Extract timestamp (format: YYYY-MM-DD HH:MM:SS.ffffff)
        ts_match = re.match(r'(\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d+)', line)
        if not ts_match:
            return None

        timestamp = self.parse_timestamp(ts_match.group(1))

        # Extract IP.port pairs (tcpdump format: IP.port > IP.port)
        # Pattern: XXX.XXX.XXX.XXX.NNNNN > XXX.XXX.XXX.XXX.NNNNN
        addr_match = re.search(r'([\d.]+)\.(\d+)\s*>\s*([\d.]+)\.(\d+)', line)
        if not addr_match:
            return None

        src_ip = addr_match.group(1)
        src_port = int(addr_match.group(2))
        dst_ip = addr_match.group(3)
        dst_port = int(addr_match.group(4))

        # Get protocol info
        protocol, transaction_type, payload_summary = self.get_protocol_info(
            line, src_port, dst_port
        )

        # Try to extract byte count
        byte_match = re.search(r'length\s+(\d+)', line)
        byte_count = int(byte_match.group(1)) if byte_match else 0

        packet_data = {
            'timestamp': timestamp,
            'src_ip': src_ip,
            'dst_ip': dst_ip,
            'src_port': src_port,
            'dst_port': dst_port,
            'protocol': protocol,
            'transaction_type': transaction_type,
            'payload_summary': payload_summary,
            'byte_count': byte_count,
            'raw_line': line
        }

        return packet_data

    def write_log_entry(self, formatted_entry, signatures=None):
        """Write a log entry to file, with optional failure signature annotations."""
        self.rotate_logs()

        with open(self.log_file, 'a') as f:
            f.write(formatted_entry + '\n')

            # Append failure signatures if detected
            if signatures:
                for sig in signatures:
                    confidence = sig.get('confidence', 'UNKNOWN')
                    sig_type = sig['type'].upper()
                    description = sig['description']
                    extra_info = ""

                    if 'bytes' in sig:
                        extra_info = f" ({sig['bytes']} bytes)"
                    elif 'interval_sec' in sig:
                        extra_info = f" ({sig['interval_sec']}s interval)"

                    sig_line = f"  ⚠️  [{confidence}] {sig_type}: {description}{extra_info}"
                    f.write(sig_line + '\n')

    def run(self):
        """Main loop: read tcpdump output and parse."""
        try:
            current_packet_lines = []

            for line in sys.stdin:
                line_stripped = line.strip()

                # Check if this is the start of a new packet (has timestamp)
                if re.match(r'\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}', line_stripped):
                    # Process previous packet if any
                    if current_packet_lines:
                        self.process_packet(current_packet_lines)
                    # Start new packet
                    current_packet_lines = [line_stripped]
                else:
                    # Add to current packet
                    if current_packet_lines:
                        current_packet_lines.append(line_stripped)

            # Process final packet
            if current_packet_lines:
                self.process_packet(current_packet_lines)

        except KeyboardInterrupt:
            sys.exit(0)
        except Exception as e:
            with open(self.log_file, 'a') as f:
                f.write(f"[{datetime.now()}] ERROR: {str(e)}\n")
            raise

    def process_packet(self, packet_lines):
        """Process a complete packet (header + ASCII lines)."""
        if not packet_lines:
            return

        header_line = packet_lines[0]
        payload_lines = packet_lines[1:] if len(packet_lines) > 1 else []

        # Reconstruct full packet text for analysis
        full_packet_text = ' '.join(packet_lines)

        packet_data = self.parse_tcpdump_output(header_line)
        if not packet_data:
            return

        # Extract payload from ASCII lines
        payload_summary = self.extract_payload_from_lines(payload_lines)
        if payload_summary:
            packet_data['payload_summary'] = payload_summary

        # Detect failure signatures
        signatures = self.detect_failure_signature(
            full_packet_text,
            packet_data['protocol'],
            packet_data['src_ip'],
            packet_data['dst_ip'],
            packet_data['src_port'],
            packet_data['dst_port']
        )

        # Format and write log entry
        formatted_entry = self.format_log_entry(header_line, packet_data)
        self.write_log_entry(formatted_entry, signatures if signatures else None)

        # Also print to stdout for real-time monitoring
        sys.stdout.write(formatted_entry + '\n')
        if signatures:
            for sig in signatures:
                sys.stdout.write(f"  ⚠️  [{sig.get('confidence', 'UNKNOWN')}] {sig['type'].upper()}: {sig['description']}\n")
        sys.stdout.flush()

    def extract_payload_from_lines(self, payload_lines):
        """Extract meaningful ASCII payload from tcpdump ASCII output."""
        payload_parts = []

        for line in payload_lines:
            # Skip tcpdump metadata lines
            if line.startswith('0x') or not line or line.startswith('[') or line.startswith('E '):
                continue

            # Extract readable text
            # Match sequences of printable characters
            text_match = re.search(r'([A-Za-z0-9\-_/.:\[\](){}<>@,?;=!&\'"+=*% ]{5,})', line)
            if text_match:
                text = text_match.group(1).strip()
                # Filter out common tcpdump noise
                if len(text) > 3 and not re.match(r'^[.]+(.*)[.]+$', text):
                    payload_parts.append(text[:100])  # Limit to 100 chars per line

        # Return first 80 chars of combined payload
        combined = ' '.join(payload_parts)
        return combined[:80] if combined else ""

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: parse_traffic.py <log_file>", file=sys.stderr)
        sys.exit(1)

    log_file = sys.argv[1]
    parser = NetworkTrafficParser(log_file)
    parser.run()
