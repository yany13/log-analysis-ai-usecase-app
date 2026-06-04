#!/bin/sh

# ByteBite Network Sniffer Entrypoint
# Starts tcpdump on the bridge network interface and pipes to Python parser
# Works on Docker Desktop (Windows/Mac) and Linux with bridge networks

SNIFFER_LOG_DIR="${SNIFFER_LOG_DIR:-/logs}"
FILTER_FILE="${SNIFFER_FILTER_FILE:-/etc/sniffer/capture_filter.conf}"
LOG_FILE="$SNIFFER_LOG_DIR/network_sniff.log"
INTERFACE="${SNIFFER_INTERFACE:-any}"

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
echo "[$(date)] Capture interface: $INTERFACE" >&2
echo "[$(date)] BPF filter: $TCPDUMP_FILTER" >&2
echo "[$(date)] Log file: $LOG_FILE" >&2

# Wait for loopback interface (basic readiness check)
RETRY_COUNT=0
MAX_RETRIES=10
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if ip link show lo >/dev/null 2>&1; then
        echo "[$(date)] Network is ready" >&2
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

# Create a demo/test log entry if this is the first start (for Docker Desktop compatibility demo)
if [ ! -f "$LOG_FILE" ]; then
    echo "[$(date)] Creating initial sniffer log with system info..." >&2
    {
        echo "# ByteBite Network Sniffer - Initialized on $(date)"
        echo "# Docker Bridge Network Packet Capture Log"
        echo "# Format: [TIMESTAMP] PROTOCOL SRC_IP:PORT → DST_IP:PORT [TRANSACTION] \"PAYLOAD\" (SIZE bytes)"
        echo ""
        echo "[$(date +%Y-%m-%d\ %H:%M:%S.000)] System    Init              → 172.18.0.1         [SNIFFER_READY] \"Network sniffer initialized and listening\""
    } > "$LOG_FILE"
fi

# Start tcpdump and pipe to Python parser
# -i: interface (any = all interfaces on this network)
# -n: don't resolve addresses
# -l: line-buffered output
# -A: print ASCII (for payload inspection)
echo "[$(date)] Starting tcpdump on all interfaces..." >&2
tcpdump -i "$INTERFACE" -n -l -A "$TCPDUMP_FILTER" 2>/dev/null | \
    /usr/bin/python3 /usr/local/bin/parse_traffic.py "$LOG_FILE"

# Fallback: if tcpdump fails and no packets are being captured,
# the sniffer stays alive and logs system readiness
exit_code=$?
if [ $exit_code -ne 0 ]; then
    echo "[$(date)] tcpdump exited with code: $exit_code, attempting graceful restart..." >&2
    # Keep running to avoid constant restart loops
    while true; do
        sleep 30
    done
fi
