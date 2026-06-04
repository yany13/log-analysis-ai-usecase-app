FROM tomcat:10-jdk21-temurin

# Set metadata
LABEL maintainer="ByteBite Team"
LABEL description="ByteBite Log Analysis AI - Tomcat Application Server"

# Set environment variables
ENV CATALINA_HOME=/usr/local/tomcat \
    CATALINA_BASE=/usr/local/tomcat \
    CATALINA_OPTS="-Djava.awt.headless=true -XX:+UseG1GC -XX:MaxGCPauseMillis=200" \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"

# Install additional dependencies and networking utilities
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    wget \
    openssl \
    ca-certificates \
    net-tools \
    netcat-openbsd \
    tcpdump \
    postgresql-client \
    ldap-utils \
    && rm -rf /var/lib/apt/lists/*

# Create keystore directory
RUN mkdir -p ${CATALINA_HOME}/conf/security

# Remove default webapps
RUN rm -rf ${CATALINA_HOME}/webapps/ROOT \
    && rm -rf ${CATALINA_HOME}/webapps/examples \
    && rm -rf ${CATALINA_HOME}/webapps/manager \
    && rm -rf ${CATALINA_HOME}/webapps/host-manager \
    && rm -rf ${CATALINA_HOME}/webapps/docs

# Create directories for logs and temp files
RUN mkdir -p ${CATALINA_HOME}/logs \
    && mkdir -p ${CATALINA_HOME}/work \
    && mkdir -p ${CATALINA_HOME}/temp

# Copy server configuration
COPY docker/tomcat/server.xml ${CATALINA_HOME}/conf/server.xml

# Copy logging configuration for ByteBite application logging
COPY docker/tomcat/logging.properties ${CATALINA_HOME}/conf/logging.properties

# Copy keystore (should be volume-mounted at runtime, this is fallback)
# COPY docker/tomcat/keystore.jks ${CATALINA_HOME}/conf/keystore.jks

# Copy context configuration for bytebite app
RUN mkdir -p ${CATALINA_HOME}/conf/Catalina/localhost
COPY docker/tomcat/context.xml ${CATALINA_HOME}/conf/Catalina/localhost/bytebite.xml

# Copy initialization files (from adhoc container consolidation)
RUN mkdir -p /ldif ${CATALINA_HOME}/docker-init
COPY docker/adhoc/bootstrap.ldif /ldif/bootstrap.ldif
COPY docker/postgres/init.sql ${CATALINA_HOME}/docker-init/init.sql

# Copy entrypoint script
COPY docker/tomcat/entrypoint.sh /usr/local/bin/entrypoint.sh

# Set correct permissions
RUN chmod -R 755 ${CATALINA_HOME}/logs \
    && chmod -R 755 ${CATALINA_HOME}/temp \
    && chmod -R 755 ${CATALINA_HOME}/work \
    && chmod 644 ${CATALINA_HOME}/conf/server.xml \
    && chmod +x /usr/local/bin/entrypoint.sh \
    && chmod +x ${CATALINA_HOME}/bin/catalina.sh

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/bytebite/ || exit 1

# Expose ports
EXPOSE 8080 8443 8009

# Set working directory
WORKDIR ${CATALINA_HOME}

# Use entrypoint script for initialization + Tomcat startup
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
