FROM osixia/openldap:1.5.0

# Install networking utilities (fix old package sources)
RUN sed -i 's/deb.debian.org/archive.debian.org/g' /etc/apt/sources.list && \
    sed -i 's/security.debian.org/archive.debian.org/g' /etc/apt/sources.list && \
    sed -i 's/ftp.debian.org/archive.debian.org/g' /etc/apt/sources.list && \
    sed -i '/backports/d' /etc/apt/sources.list && \
    apt-get update && apt-get install -y --no-install-recommends \
    net-tools \
    netcat-openbsd \
    tcpdump \
    curl \
    && rm -rf /var/lib/apt/lists/*
