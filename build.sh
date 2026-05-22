#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# build.sh  —  run this on YOUR MAC to build & push a new image to Docker Hub
# Usage: ./build.sh
# ─────────────────────────────────────────────────────────────────────────────
set -e

DOCKERHUB_USER="hoyi9749"   # ← change this once
IMAGE="$DOCKERHUB_USER/finance-tracker-backend"

echo "🔨  Building $IMAGE:$TAG ..."
docker build --platform linux/amd64 -t "$IMAGE:latest" .

echo "📤  Pushing to Docker Hub ..."
docker push "$IMAGE:latest"

echo ""
echo "✅  Done!"
echo "    Image : $IMAGE:latest"
echo ""
echo "    On your server run:"
echo "    ./deploy.sh"
