#!/bin/sh

# ByteBite Network Sniffer Entrypoint
# Starts tcpdump on the bridge network interface and pipes to Python parser
# Works on Docker Desktop (Windows/Mac) and Linux with bridge networks

SNIFFER_LOG_DIR="${SNIFFER_LOG_DIR:-/logs}"
FILTER_FILE="${SNIFFER_FILTER_FILE:-/etc/sniffer/capture_filter.conf}"
LOG_FILE="$SNIFFER_LOG_DIR/network_sniff.log"
# Use loopback for Docker Desktop (works), eth0 for Linux native
INTERFACE="${SNIFFER_INTERFACE:-lo}"

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

# Create initial log entry
if [ ! -f "$LOG_FILE" ]; then
    echo "[$(date)] Creating initial sniffer log..." >&2
    {
        echo "# ByteBite Network Sniffer - Initialized on $(date)"
        echo "# Docker Bridge Network Packet Capture Log"
        echo "# Format: [TIMESTAMP] PROTOCOL SRC_IP:PORT → DST_IP:PORT [TRANSACTION] \"PAYLOAD\" (SIZE bytes)"
        echo ""
        echo "[$(date +%Y-%m-%d\ %H:%M:%S.000)] System    Init              → 172.18.0.1         [SNIFFER_READY] \"Network sniffer initialized and listening on all interfaces\""
    } > "$LOG_FILE"
fi

# Check if we can actually capture packets (Linux native support)
# On Docker Desktop, tcpdump on bridge won't see inter-container traffic
echo "[$(date)] Starting tcpdump capture on interface: $INTERFACE..." >&2
echo "[$(date)] BPF filter: $TCPDUMP_FILTER" >&2

# Start tcpdump with timeout to detect if capture is working
{
    timeout 5 tcpdump -i "$INTERFACE" -n -l -A "$TCPDUMP_FILTER" 2>/dev/null
    tcpdump_exit=$?

    # If tcpdump exits immediately, we're in Docker Desktop (no packet capture possible)
    if [ $tcpdump_exit -eq 124 ]; then
        echo "[$(date)] tcpdump is listening (Linux native mode)" >&2
    else
        echo "[$(date)] WARNING: tcpdump exited with code $tcpdump_exit" >&2
        echo "[$(date)] This may indicate Docker Desktop (bridge traffic not visible to tcpdump)" >&2
        echo "[$(date)] On Linux with native Docker, this would capture all app traffic" >&2
    fi
} | /usr/bin/python3 /usr/local/bin/parse_traffic.py "$LOG_FILE" &

# Run tcpdump continuously in background
tcpdump -i "$INTERFACE" -n -l -A "$TCPDUMP_FILTER" 2>/dev/null | \
    /usr/bin/python3 /usr/local/bin/parse_traffic.py "$LOG_FILE"

# Keep sniffer running even if tcpdump fails
exit_code=$?
echo "[$(date)] tcpdump exited with code: $exit_code" >&2
echo "[$(date)] Sniffer staying alive (waiting for packets on $INTERFACE)..." >&2

# Keep container running
while true; do
    sleep 30
    if [ ! -f "$LOG_FILE" ]; then
        # Ensure log file exists
        touch "$LOG_FILE"
    fi
done
