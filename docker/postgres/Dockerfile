FROM postgres:15-alpine

# Install networking utilities
RUN apk add --no-cache \
    net-tools \
    netcat-openbsd \
    tcpdump \
    curl

COPY init.sql /docker-entrypoint-initdb.d/
