#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# run-local.sh  —  Build and run backend in Docker on your Mac
#
# Prerequisites: mysql, redis, minio containers already running
# Usage:  bash run-local.sh
# ─────────────────────────────────────────────────────────────────────────────
set -e

IMAGE="finance-tracker-backend:local"
CONTAINER="finance-tracker"

echo "🔨  Building image ..."
docker build -t $IMAGE .

# Stop old container if running
docker rm -f $CONTAINER 2>/dev/null && echo "🗑️   Removed old container" || true

echo "🚀  Starting backend ..."
docker run -d \
  --name $CONTAINER \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/financeTracker?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  -e MYSQL_PASSWORD="Zz7763882336^" \
  -e SPRING_DATA_REDIS_HOST="host.docker.internal" \
  -e REDIS_PASSWORD="Zz7763882336^" \
  -e JWT_SECRET="dev-secret-key-at-least-32-characters-long!!" \
  -e GOOGLE_CLIENT_ID="180806298382-q7ofrc16oqep2k7jsd5budl10o1qkqn6.apps.googleusercontent.com" \
  $IMAGE

echo ""
echo "📋  Logs (Ctrl+C to stop watching, container keeps running):"
docker logs -f $CONTAINER
