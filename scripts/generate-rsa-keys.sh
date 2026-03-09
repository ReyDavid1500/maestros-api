#!/usr/bin/env bash
# =============================================================================
# generate-rsa-keys.sh
#
# Generates an RSA-2048 key pair for JWT RS256 signing and exports them as
# base64-encoded environment variables ready to paste into your .env file.
#
# Requirements: openssl (pre-installed on macOS/Linux)
#
# Usage:
#   chmod +x scripts/generate-rsa-keys.sh
#   ./scripts/generate-rsa-keys.sh
#
# The generated .pem files are listed in .gitignore — NEVER commit them.
# Only commit .env.example (without real values).
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="$SCRIPT_DIR/../keys"

mkdir -p "$OUT_DIR"

PRIVATE_KEY="$OUT_DIR/private.pem"
PUBLIC_KEY="$OUT_DIR/public.pem"

echo ""
echo "▶  Generating RSA-2048 private key..."
openssl genrsa -out "$PRIVATE_KEY" 2048 2>/dev/null

echo "▶  Extracting public key..."
openssl rsa -in "$PRIVATE_KEY" -pubout -out "$PUBLIC_KEY" 2>/dev/null

echo ""
echo "✅  Keys written to:"
echo "    Private : $PRIVATE_KEY"
echo "    Public  : $PUBLIC_KEY"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Add the following to your .env file (keep it secret!)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

PRIVATE_B64=$(base64 -i "$PRIVATE_KEY" | tr -d '\n')
PUBLIC_B64=$(base64 -i "$PUBLIC_KEY" | tr -d '\n')

echo "JWT_PRIVATE_KEY=$PRIVATE_B64"
echo ""
echo "JWT_PUBLIC_KEY=$PUBLIC_B64"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "⚠️  The .pem files in keys/ are gitignored."
echo "    Store the base64 values in your password manager or"
echo "    secret manager (e.g. AWS Secrets Manager, Vault)."
echo "    Delete the .pem files when done:"
echo "    rm -rf $OUT_DIR"
echo ""
