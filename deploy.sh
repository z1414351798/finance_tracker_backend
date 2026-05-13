#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# deploy.sh  —  run on YOUR SERVER to update the backend image
# Full first-time setup:  ./deploy.sh --first-time
# Normal update:          ./deploy.sh
# ─────────────────────────────────────────────────────────────────────────────
set -e

DOCKERHUB_USER="hoyi9749"
IMAGE="$DOCKERHUB_USER/finance-tracker-backend"

if [ "$1" == "--first-time" ]; then
    echo "🔐  Generating SSL certificates ..."
    chmod +x nginx/gen-certs.sh && bash nginx/gen-certs.sh

    echo "🚀  Starting all services for the first time ..."
    docker compose pull
    docker compose up -d

    echo ""
    echo "✅  All services started!"
    docker compose ps
else
    echo "📥  Pulling latest backend image ..."
    docker compose pull backend

    echo "🔄  Restarting backend only ..."
    docker compose up -d --no-deps backend

    echo ""
    echo "✅  Backend updated!"
    docker compose ps backend
fi
