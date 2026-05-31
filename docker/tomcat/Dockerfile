FROM tomcat:10-jdk21-temurin

# Set metadata
LABEL maintainer="ByteBite Team"
LABEL description="ByteBite Log Analysis AI - Tomcat Application Server"

# Set environment variables
ENV CATALINA_HOME=/usr/local/tomcat \
    CATALINA_BASE=/usr/local/tomcat \
    CATALINA_OPTS="-Djava.awt.headless=true -XX:+UseG1GC -XX:MaxGCPauseMillis=200" \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"

# Install additional dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    wget \
    openssl \
    ca-certificates \
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
COPY server.xml ${CATALINA_HOME}/conf/server.xml

# Copy logging configuration for ByteBite application logging
COPY logging.properties ${CATALINA_HOME}/conf/logging.properties

# Copy keystore (should be volume-mounted at runtime, this is fallback)
# COPY keystore.jks ${CATALINA_HOME}/conf/keystore.jks

# Copy context configuration for bytebite app (optional - for specific app config)
RUN mkdir -p ${CATALINA_HOME}/conf/Catalina/localhost
COPY context.xml ${CATALINA_HOME}/conf/Catalina/localhost/bytebite.xml 

# Set correct permissions
RUN chmod -R 755 ${CATALINA_HOME}/logs \
    && chmod -R 755 ${CATALINA_HOME}/temp \
    && chmod -R 755 ${CATALINA_HOME}/work \
    && chmod 644 ${CATALINA_HOME}/conf/server.xml

# Copy startup script
# COPY startup.sh ${CATALINA_HOME}/bin/startup.sh
# RUN chmod +x ${CATALINA_HOME}/bin/startup.sh \
#    && chmod +x ${CATALINA_HOME}/bin/catalina.sh

RUN chmod +x ${CATALINA_HOME}/bin/catalina.sh

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/bytebite/ || exit 1

# Expose ports
EXPOSE 8080 8443 8009

# Set working directory
WORKDIR ${CATALINA_HOME}

# Run Tomcat
CMD ["catalina.sh", "run"]
