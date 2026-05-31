#!/bin/bash
set -e

echo "Starting ByteBite Tomcat Server..."

# Run any pre-startup commands here if needed 
# (e.g., waiting for database, checking config files)

# Hand execution over to the original Tomcat startup process
exec catalina.sh run