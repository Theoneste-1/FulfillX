#!/usr/bin/env bash
# First-time VPS bootstrap. Run on the server as a user in the docker group.
set -euo pipefail

APP_DIR="${DEPLOY_PATH:-/opt/fulfillx}"

sudo mkdir -p "$APP_DIR"
sudo chown "$USER:$USER" "$APP_DIR"

if [[ ! -f "$APP_DIR/.env" ]]; then
  if [[ -f "$APP_DIR/deploy/.env.example" ]]; then
    cp "$APP_DIR/deploy/.env.example" "$APP_DIR/.env"
    echo "Created $APP_DIR/.env from the example. Edit secrets before starting."
  else
    echo "Copy deploy/.env.example to $APP_DIR/.env and edit it, then re-run."
    exit 1
  fi
fi

echo "Next:"
echo "  1. Edit $APP_DIR/.env (IMAGE_REGISTRY, JWT_SECRET, passwords, domains)"
echo "  2. docker login ghcr.io"
echo "  3. bash $APP_DIR/deploy/server-up.sh"
echo "  4. Install nginx from infrastructure/nginx (see docs/deploy.md)"
