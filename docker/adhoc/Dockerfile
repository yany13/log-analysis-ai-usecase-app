FROM alpine:latest

# Install required tools for initialization
RUN apk add --no-cache \
    openldap-clients \
    curl \
    wget \
    bash \
    postgresql-client

# Copy LDIF bootstrap file
COPY docker/adhoc/bootstrap.ldif /ldif/bootstrap.ldif

# Copy initialization scripts
COPY scripts/ /scripts/

# Make scripts executable
RUN chmod +x /scripts/*.sh && \
    chmod 644 /ldif/bootstrap.ldif

# Set entrypoint
ENTRYPOINT ["/scripts/init.sh"]
