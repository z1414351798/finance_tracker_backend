#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# setup-ssl.sh  —  Get a free trusted Let's Encrypt certificate
#
# Usage (run on your server, from the project root):
#   bash nginx/setup-ssl.sh yourdomain.com you@email.com
#
# Requires: docker, port 80 free (script stops nginx temporarily)
# After this runs, nginx will serve HTTPS with a certificate trusted by
# all browsers and Android devices — no warnings, no extra Android config.
# ─────────────────────────────────────────────────────────────────────────────
set -e

DOMAIN="${1}"
EMAIL="${2}"

if [ -z "$DOMAIN" ] || [ -z "$EMAIL" ]; then
    echo "Usage: bash nginx/setup-ssl.sh <domain> <email>"
    echo "  e.g. bash nginx/setup-ssl.sh api.myapp.com me@gmail.com"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CERTS_DIR="$SCRIPT_DIR/certs"
LE_DIR="$SCRIPT_DIR/letsencrypt"

echo "🛑  Stopping nginx to free port 80 ..."
docker compose stop nginx

echo "🔐  Requesting Let's Encrypt certificate for $DOMAIN ..."
docker run --rm \
    -p 80:80 \
    -v "$LE_DIR:/etc/letsencrypt" \
    certbot/certbot certonly \
    --standalone \
    --non-interactive \
    --agree-tos \
    --email "$EMAIL" \
    -d "$DOMAIN"

echo "📋  Copying certs into nginx/certs/ ..."
mkdir -p "$CERTS_DIR"
cp "$LE_DIR/live/$DOMAIN/fullchain.pem" "$CERTS_DIR/cert.pem"
cp "$LE_DIR/live/$DOMAIN/privkey.pem"   "$CERTS_DIR/key.pem"
chmod 644 "$CERTS_DIR/cert.pem"
chmod 600 "$CERTS_DIR/key.pem"

echo "🚀  Starting nginx with new certificate ..."
docker compose start nginx

echo ""
echo "✅  HTTPS is live!"
echo "    Test: curl https://$DOMAIN/test"
echo ""
echo "⚠️  Certificate expires in 90 days."
echo "    Run 'bash nginx/renew-ssl.sh $DOMAIN' before then to renew."
