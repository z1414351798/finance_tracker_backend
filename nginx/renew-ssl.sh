#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# renew-ssl.sh  —  Renew Let's Encrypt certificate before it expires
#
# Let's Encrypt certs last 90 days. Run this every ~60 days.
# Usage (from the project root):
#   bash nginx/renew-ssl.sh yourdomain.com
#
# Tip: set up a monthly cron job on your server:
#   0 3 1 * * cd /path/to/project && bash nginx/renew-ssl.sh yourdomain.com
# ─────────────────────────────────────────────────────────────────────────────
set -e

DOMAIN="${1}"

if [ -z "$DOMAIN" ]; then
    echo "Usage: bash nginx/renew-ssl.sh <domain>"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CERTS_DIR="$SCRIPT_DIR/certs"
LE_DIR="$SCRIPT_DIR/letsencrypt"

echo "🛑  Stopping nginx to free port 80 ..."
docker compose stop nginx

echo "🔄  Renewing certificate for $DOMAIN ..."
docker run --rm \
    -p 80:80 \
    -v "$LE_DIR:/etc/letsencrypt" \
    certbot/certbot renew \
    --standalone \
    --non-interactive

echo "📋  Copying renewed certs ..."
cp "$LE_DIR/live/$DOMAIN/fullchain.pem" "$CERTS_DIR/cert.pem"
cp "$LE_DIR/live/$DOMAIN/privkey.pem"   "$CERTS_DIR/key.pem"
chmod 644 "$CERTS_DIR/cert.pem"
chmod 600 "$CERTS_DIR/key.pem"

echo "🚀  Restarting nginx ..."
docker compose start nginx

echo "✅  Certificate renewed!"
