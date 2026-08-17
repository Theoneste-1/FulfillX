#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ ! -f .env ]]; then
  echo "Missing $ROOT/.env — copy deploy/.env.example and fill in secrets." >&2
  exit 1
fi

# shellcheck disable=SC1091
_ci_tag="${IMAGE_TAG:-}"
_ci_registry="${IMAGE_REGISTRY:-}"
set -a
source .env
set +a
[[ -n "$_ci_tag" ]] && IMAGE_TAG="$_ci_tag"
[[ -n "$_ci_registry" ]] && IMAGE_REGISTRY="$_ci_registry"
export IMAGE_TAG IMAGE_REGISTRY

: "${IMAGE_REGISTRY:?set IMAGE_REGISTRY in .env}"
: "${IMAGE_TAG:=latest}"
: "${JWT_SECRET:?set JWT_SECRET in .env}"
: "${POSTGRES_PASSWORD:?set POSTGRES_PASSWORD in .env}"
: "${APP_ORIGIN:?set APP_ORIGIN in .env}"

if [[ -n "${GHCR_TOKEN:-}" && -n "${GHCR_USER:-}" ]]; then
  echo "${GHCR_TOKEN}" | docker login ghcr.io -u "${GHCR_USER}" --password-stdin
fi

docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d --remove-orphans
docker compose -f docker-compose.prod.yml ps
