FROM alpine:3.19

# Install tcpdump, python3, and networking utilities
RUN apk add --no-cache \
    tcpdump \
    python3 \
    curl \
    iproute2 \
    grep \
    netcat-openbsd \
    net-tools

# Create log directory
RUN mkdir -p /logs

# Copy scripts
COPY ./entrypoint.sh /usr/local/bin/entrypoint.sh
COPY ./parse_traffic.py /usr/local/bin/parse_traffic.py
COPY ./simulate_traffic.py /usr/local/bin/simulate_traffic.py

# Make scripts executable
RUN chmod +x /usr/local/bin/entrypoint.sh \
    && chmod +x /usr/local/bin/parse_traffic.py \
    && chmod +x /usr/local/bin/simulate_traffic.py

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
