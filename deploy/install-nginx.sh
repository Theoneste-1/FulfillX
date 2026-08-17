#!/usr/bin/env bash
# Point the nginx site files at your domains. Does not install TLS — run certbot after DNS works.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NGINX_DIR="$ROOT/infrastructure/nginx"

API_HOST="${API_HOST:-api.example.com}"
APP_HOST="${APP_HOST:-app.example.com}"
GRAFANA_HOST="${GRAFANA_HOST:-grafana.example.com}"
SINGLE_HOST="${SINGLE_HOST:-fulfillx.example.com}"
MODE="${1:-split}"

sudo mkdir -p /etc/nginx/snippets /etc/nginx/sites-available /etc/nginx/sites-enabled
sudo cp "$NGINX_DIR/proxy-headers.conf" /etc/nginx/snippets/fulfillx-proxy.conf

install_site() {
  local src="$1" dest="$2" from="$3" to="$4"
  sudo cp "$src" "/etc/nginx/sites-available/${dest}"
  sudo sed -i "s/${from}/${to}/g" "/etc/nginx/sites-available/${dest}"
  sudo ln -sfn "/etc/nginx/sites-available/${dest}" "/etc/nginx/sites-enabled/${dest}"
}

if [[ "$MODE" == "single" ]]; then
  install_site "$NGINX_DIR/single-host.conf" fulfillx.conf fulfillx.example.com "$SINGLE_HOST"
  sudo rm -f /etc/nginx/sites-enabled/fulfillx-api.conf /etc/nginx/sites-enabled/fulfillx-app.conf
else
  install_site "$NGINX_DIR/api.conf" fulfillx-api.conf api.example.com "$API_HOST"
  install_site "$NGINX_DIR/app.conf" fulfillx-app.conf app.example.com "$APP_HOST"
  install_site "$NGINX_DIR/grafana.conf" fulfillx-grafana.conf grafana.example.com "$GRAFANA_HOST"
  sudo rm -f /etc/nginx/sites-enabled/fulfillx.conf
fi

sudo nginx -t
sudo systemctl reload nginx
echo "Nginx reloaded. After DNS A records exist, issue certificates:"
if [[ "$MODE" == "single" ]]; then
  echo "  sudo certbot --nginx -d ${SINGLE_HOST}"
else
  echo "  sudo certbot --nginx -d ${API_HOST} -d ${APP_HOST} -d ${GRAFANA_HOST}"
fi
