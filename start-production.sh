#!/bin/bash

# Production Startup Script for DevSync Standup Bot
# Usage: ./start-production.sh

set -e

echo "========================================="
echo " DevSync Standup Bot - Production Start"
echo "========================================="

# Check if .env exists
if [ ! -f .env ]; then
    echo "ERROR: .env file not found!"
    echo "Please copy .env.template to .env and configure it."
    echo ""
    echo "  cp .env.template .env"
    echo "  nano .env"
    echo ""
    exit 1
fi

# Load environment variables
echo "Loading environment variables..."
export $(cat .env | grep -v '^#' | xargs)

# Check Java version
echo "Checking Java version..."
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "ERROR: Java 17 or higher is required"
    echo "Current version: $JAVA_VERSION"
    exit 1
fi
echo "✓ Java version OK"

# Check if JAR exists
if [ ! -f target/standup-bot-1.0.0.jar ]; then
    echo "Building application..."
    mvn clean package -DskipTests
fi

# Check database connectivity
echo "Checking database connectivity..."
if command -v mysql &> /dev/null; then
    mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -e "SELECT 1" &> /dev/null
    if [ $? -eq 0 ]; then
        echo "✓ Database connection OK"
    else
        echo "WARNING: Cannot connect to database"
        echo "Make sure MySQL is running and credentials are correct"
    fi
else
    echo "⚠ MySQL client not found, skipping database check"
fi

# Set JVM options for production
export JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Set Spring profile
export SPRING_PROFILES_ACTIVE=production

echo ""
echo "Starting application..."
echo "Port: $SERVER_PORT"
echo "Profile: production"
echo "Logs: logs/application.log"
echo ""

# Create logs directory
mkdir -p logs

# Start application
nohup java $JAVA_OPTS \
    -Dspring.profiles.active=production \
    -jar target/standup-bot-1.0.0.jar \
    > logs/application.log 2>&1 &

PID=$!
echo $PID > standup-bot.pid

echo "✓ Application started with PID: $PID"
echo ""
echo "Commands:"
echo "  View logs:    tail -f logs/application.log"
echo "  Check status: curl http://localhost:$SERVER_PORT/api/standup/health"
echo "  Stop app:     kill \$(cat standup-bot.pid)"
echo ""
echo "Waiting for startup..."
sleep 5

# Health check
if curl -s http://localhost:$SERVER_PORT/api/standup/health > /dev/null 2>&1; then
    echo "✓ Application is healthy!"
else
    echo "⚠ Application may still be starting..."
    echo "  Check logs: tail -f logs/application.log"
fi

echo ""
echo "========================================="
echo " Application started successfully!"
echo "========================================="
