#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# deploy.sh  —  run on YOUR SERVER (CentOS)
#
# First time:    bash deploy.sh --first-time
# Normal update: bash deploy.sh
# ─────────────────────────────────────────────────────────────────────────────
set -e

DOMAIN="www.wisefintrakr.com"

if [ "$1" == "--first-time" ]; then
    echo "🚀  Starting all services ..."
    docker compose pull
    docker compose up -d

    echo ""
    echo "✅  Done! Your app is live at https://$DOMAIN"
    docker compose ps
else
    echo "📥  Pulling latest backend image ..."
    docker compose pull backend

    echo "🔄  Restarting backend ..."
    docker compose up -d --no-deps backend

    echo ""
    echo "✅  Backend updated!"
    docker compose ps backend
fi
