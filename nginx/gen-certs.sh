#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# Generates a self-signed SSL certificate (valid 10 years).
# Run once on your server before starting docker-compose.
#
# For production with a real domain, use Let's Encrypt instead:
#   sudo apt install certbot
#   sudo certbot certonly --standalone -d yourdomain.com
#   Then copy:
#     /etc/letsencrypt/live/yourdomain.com/fullchain.pem → certs/cert.pem
#     /etc/letsencrypt/live/yourdomain.com/privkey.pem  → certs/key.pem
# ─────────────────────────────────────────────────────────────────────────────
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CERTS_DIR="$SCRIPT_DIR/certs"

mkdir -p "$CERTS_DIR"

openssl req -x509 -nodes -days 3650 \
  -newkey rsa:2048 \
  -keyout "$CERTS_DIR/key.pem" \
  -out    "$CERTS_DIR/cert.pem" \
  -subj   "/C=HK/ST=HK/L=HK/O=FinanceTracker/CN=localhost"

echo "✅  Certs generated:"
echo "    $CERTS_DIR/cert.pem"
echo "    $CERTS_DIR/key.pem"
