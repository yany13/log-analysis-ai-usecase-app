#!/bin/bash

# ByteBite Network Sniffer Entrypoint
# Starts tcpdump on the bridge network interface and pipes to Python parser
# Works on Docker Desktop (Windows/Mac) and Linux with bridge networks

SNIFFER_LOG_DIR="${SNIFFER_LOG_DIR:-/logs}"
FILTER_FILE="${SNIFFER_FILTER_FILE:-/etc/sniffer/capture_filter.conf}"
LOG_FILE="$SNIFFER_LOG_DIR/network_sniff.log"
INTERFACE="${SNIFFER_INTERFACE:-eth0}"

# Ensure log directory exists
mkdir -p "$SNIFFER_LOG_DIR"

# Load BPF filter from config file
if [ ! -f "$FILTER_FILE" ]; then
    echo "[$(date)] ERROR: Filter configuration file not found: $FILTER_FILE" >&2
    exit 1
fi

# Read filter (remove comments and empty lines)
TCPDUMP_FILTER=$(grep -v '^#' "$FILTER_FILE" | grep -v '^$' | tr '\n' ' ')

if [ -z "$TCPDUMP_FILTER" ]; then
    echo "[$(date)] ERROR: No valid BPF filter found in $FILTER_FILE" >&2
    exit 1
fi

echo "[$(date)] ByteBite Network Sniffer Starting" >&2
echo "[$(date)] Log directory: $SNIFFER_LOG_DIR" >&2
echo "[$(date)] Network interface: $INTERFACE" >&2
echo "[$(date)] BPF filter: $TCPDUMP_FILTER" >&2
echo "[$(date)] Log file: $LOG_FILE" >&2

# Wait for network interface to be ready (max 30 seconds)
RETRY_COUNT=0
MAX_RETRIES=30
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if ip link show "$INTERFACE" >/dev/null 2>&1; then
        echo "[$(date)] Network interface $INTERFACE is ready" >&2
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    sleep 1
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo "[$(date)] WARNING: Network interface not ready after 30s, attempting capture anyway..." >&2
fi

# Trap SIGTERM to gracefully shutdown
trap 'echo "[$(date)] Received SIGTERM, shutting down..." >&2; exit 0' SIGTERM

# Start tcpdump and pipe to Python parser
# -i: interface
# -n: don't resolve addresses
# -l: line-buffered output
# -A: print ASCII (for payload inspection)
echo "[$(date)] Starting tcpdump on $INTERFACE..." >&2
tcpdump -i "$INTERFACE" -n -l -A "$TCPDUMP_FILTER" 2>/dev/null | \
    /usr/bin/python3 /usr/local/bin/parse_traffic.py "$LOG_FILE"

# Script exits if tcpdump fails
exit_code=$?
echo "[$(date)] Sniffer exited with code: $exit_code" >&2
exit $exit_code
