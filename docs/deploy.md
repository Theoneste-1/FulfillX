# Production deploy (GitHub Actions → GHCR → VPS → Nginx)

This is the path from `git push` to a public hostname. Local `docker compose up --build` is unchanged.

```text
  git push origin main
           │
           ▼
  GitHub Actions (CD)
    1. mvn test
    2. mvn package
    3. docker buildx bake --push  →  ghcr.io/<you>/fullfix/<service>:<sha>
           │
           ▼  (only if ENABLE_DEPLOY=true)
  SSH to the VPS
    docker login ghcr.io
    docker compose -f docker-compose.prod.yml pull && up -d
           │
           ▼
  Nginx on the host
    app.example.com  →  127.0.0.1:3000  (dashboard)
    api.example.com  →  127.0.0.1:8080  (gateway)
    grafana.example.com → 127.0.0.1:3001
```

Postgres, Redis, and Kafka stay on the Docker network. They are **not** published to the internet.

---

## 1. Put the repo on GitHub

```bash
git remote add origin https://github.com/<you>/fullfix.git
git push -u origin main
```

GitHub Actions needs permission to write packages:

- Repo → **Settings → Actions → General → Workflow permissions** → Read and write
- After the first image push, **Packages** on the GitHub user/org will list `fullfix/auth-service`, etc.

Images are private by default. The deploy job logs into GHCR with `GITHUB_TOKEN`. For a manual pull on the server, create a classic PAT with `read:packages` (and SSO authorize it if the org requires that).

---

## 2. What CI/CD does

| Workflow | When | What |
|---|---|---|
| `.github/workflows/ci.yml` | PRs and non-`main` pushes | `mvn test` (unit tests; Testcontainers ITs stay excluded) |
| `.github/workflows/cd.yml` | Push to `main`, or **Run workflow** | Test → build 11 images → push to GHCR → optional SSH deploy |

Image names:

```text
ghcr.io/<owner>/fullfix/api-gateway:<git-sha>
ghcr.io/<owner>/fullfix/api-gateway:latest
… same for auth-service, catalog-service, order-service, inventory-service,
  warehouse-service, shipment-service, payment-service, notification-service,
  analytics-service, operations-dashboard
```

Rollback: set `IMAGE_TAG=<older-sha>` in `/opt/fulfillx/.env` and run `bash deploy/server-up.sh`.

---

## 3. GitHub secrets and variables

**Secrets** (Settings → Secrets and variables → Actions):

| Name | Used for |
|---|---|
| `DEPLOY_HOST` | VPS public IP or hostname |
| `DEPLOY_USER` | SSH user that can run Docker (in the `docker` group) |
| `DEPLOY_SSH_KEY` | **Private** key whose public half is in `~/.ssh/authorized_keys` on the VPS |

**Variables**:

| Name | Value |
|---|---|
| `ENABLE_DEPLOY` | `true` when the VPS is ready. Leave unset until then — images still build. |
| `DEPLOY_PATH` | optional, default `/opt/fulfillx` |

Create a GitHub **Environment** named `production` (the deploy job uses it). You can add a required reviewer later.

Generate a deploy key on your laptop:

```bash
ssh-keygen -t ed25519 -f fulfillx-deploy -C fulfillx-cd
```

Put `fulfillx-deploy.pub` on the server. Put the **private** file contents into `DEPLOY_SSH_KEY`. Do not commit the key.

---

## 4. First time on the VPS

Ubuntu/Debian, Docker Engine + Compose plugin, Nginx:

```bash
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-v2 nginx rsync curl
sudo usermod -aG docker "$USER"
# log out and back in so the docker group applies
```

Copy the repo onto the box once (Actions will rsync compose/infra after that):

```bash
sudo mkdir -p /opt/fulfillx
sudo chown "$USER:$USER" /opt/fulfillx
# from your laptop, after the first successful CD rsync — or clone:
git clone https://github.com/<you>/fullfix.git /opt/fulfillx
cp /opt/fulfillx/deploy/.env.example /opt/fulfillx/.env
nano /opt/fulfillx/.env
```

Required `.env` edits:

1. `IMAGE_REGISTRY=ghcr.io/<you>/fullfix` — lowercase GitHub owner + repo name
2. `JWT_SECRET` — new random string, **≥ 32 bytes**. Do not ship the demo secret.
3. `POSTGRES_PASSWORD` and `GRAFANA_ADMIN_PASSWORD`
4. `APP_ORIGIN=https://app.example.com` (browser origin for CORS)
5. `PUBLIC_API_URL=https://api.example.com` (what the dashboard calls)

Manual start (before Actions deploy is enabled):

```bash
echo "$GHCR_PAT" | docker login ghcr.io -u YOUR_GITHUB_USER --password-stdin
cd /opt/fulfillx
bash deploy/server-up.sh
curl -fsS http://127.0.0.1:8080/actuator/health
```

Then set `ENABLE_DEPLOY=true` so the next push to `main` deploys.

Firewall: allow 22, 80, 443 only. Compose binds the app to `127.0.0.1`, so Nginx is the only public HTTP entry.

---

## 5. Domains and Nginx

Point DNS **A records** at the VPS:

| Host | Role |
|---|---|
| `api.example.com` | API gateway |
| `app.example.com` | Operations dashboard |
| `grafana.example.com` | Grafana (optional, admin-only) |

On the server, after files exist under `/opt/fulfillx`:

```bash
export API_HOST=api.example.com
export APP_HOST=app.example.com
export GRAFANA_HOST=grafana.example.com
sudo bash /opt/fulfillx/deploy/install-nginx.sh split
```

When the A records resolve:

```bash
sudo apt-get install -y certbot python3-certbot-nginx
sudo certbot --nginx -d api.example.com -d app.example.com -d grafana.example.com
```

Certbot rewrites the site files to listen on 443 and renews via systemd.

### One hostname instead of three

If you only have `fulfillx.example.com`:

```bash
# .env
APP_ORIGIN=https://fulfillx.example.com
PUBLIC_API_URL=

export SINGLE_HOST=fulfillx.example.com
sudo bash /opt/fulfillx/deploy/install-nginx.sh single
sudo certbot --nginx -d fulfillx.example.com
```

Empty `PUBLIC_API_URL` makes the dashboard call `/api/...` on the same host. Nginx proxies `/api/` to the gateway. Recreate the dashboard container after changing `.env`:

```bash
docker compose -f docker-compose.prod.yml up -d operations-dashboard
```

The dashboard writes `/config.js` at container start from `API_BASE` / `PUBLIC_API_URL`, so you do **not** need to rebuild the image when a domain arrives.

---

## 6. Production smoke test

Use the public HTTPS hosts, not `localhost`.

```bash
# Gateway
curl -fsS https://api.example.com/actuator/health

# Login
TOKEN=$(curl -fsS -X POST https://api.example.com/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"customer@fulfillx.com","password":"Customer123!"}' \
  | jq -r .accessToken)

# Catalog
curl -fsS https://api.example.com/api/v1/products \
  -H "Authorization: Bearer $TOKEN"

# Happy path: order → poll until DELIVERED (demo shipment auto-progress)
# Idempotency-Key must be unique per create
# Body: see docs/api-design.md / docs/local-development.md

# Forced payment failure: order total ending in .13 → CANCELLED after compensation
```

Browser:

1. Open `https://app.example.com` → login `logistics@fulfillx.com` / `Operator123!`
2. Confirm orders, inventory, warehouses, shipments load
3. Place an order as `customer@fulfillx.com` / `Customer123!` (or via curl) and watch status move
4. Grafana `https://grafana.example.com` — admin password from `.env`
5. Jaeger is bound to `127.0.0.1:16686` only. SSH tunnel if you need traces:  
   `ssh -L 16686:127.0.0.1:16686 user@vps`

If the dashboard loads but API calls fail:

- Split domains: `PUBLIC_API_URL` must be `https://api.example.com` (no trailing slash) and `APP_ORIGIN` must match the dashboard origin exactly (`https://app.example.com`)
- CORS errors in the browser console mean `APP_ORIGIN` is wrong; recreate `api-gateway`
- 502 from Nginx means the container is down: `docker compose -f docker-compose.prod.yml ps` and `logs api-gateway`

Change seed passwords before you treat this as a real environment. They are documented in `docs/product-spec.md`.

---

## 7. What is still “demo” on a VPS

This stack is production-**shaped**, not a hardened SaaS:

- JWT is HS256 with a shared secret (ADR-006 wants RS256/JWKS for a real security boundary)
- Payment is a sandbox; amounts ending `.13` fail on purpose
- `FULFILLX_DEMO=true` auto-advances shipments
- Single-node Postgres / Kafka / Redis — no backups, no multi-AZ
- Grafana and Jaeger are not behind SSO

Treat TLS + private DB ports + a new JWT secret as the minimum bar before sharing the URL.

---

## 8. Kubernetes

`deployment/kubernetes/` remains the stretch path. This document is the VPS + Compose + Nginx path that matches GitHub Actions image builds.
