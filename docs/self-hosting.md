# Self-hosting OSE

OSE is designed to run as a normal Next.js application with Prisma. You can deploy it with Docker, on a VPS, or on Vercel-style platforms.

## Docker Deployment

Build and start the service:

```bash
cp .env.example .env
docker compose up -d --build
```

The included `Dockerfile` builds a standalone Next.js application and the included `docker-compose.yml` stores SQLite data in a named volume.

Minimum production environment:

```env
DATABASE_URL=file:/data/ose.db
NEXTAUTH_URL=https://your-domain.example
NEXTAUTH_SECRET=replace-with-openssl-rand-base64-32
OSE_ENCRYPTION_KEY=replace-with-openssl-rand-base64-32
```

`OSE_ENCRYPTION_KEY` is required to allow users to save their AI API keys. Without it, the settings API returns HTTP 503 when a user tries to store a key. Generate it with:

```bash
openssl rand -base64 32
```

Optional AI variables can be added to `.env` or the Compose environment section.

## Vercel Deployment

1. Import the GitHub repository in Vercel.
2. Configure environment variables:
   - `DATABASE_URL`
   - `NEXTAUTH_URL`
   - `NEXTAUTH_SECRET`
   - AI provider keys as needed.
3. OSE currently only supports SQLite. Serverless platforms like Vercel use ephemeral file systems, which are not compatible with SQLite. PostgreSQL support is on the roadmap — Vercel deployment is not recommended until PostgreSQL support is available.
4. Run Prisma migrations from CI or a trusted machine:

```bash
npx prisma migrate deploy
```

## VPS Manual Deployment

Install Node.js 22 LTS, clone the repository, then:

```bash
npm ci
npx prisma generate
npx prisma migrate deploy
npm run build
npm run start
```

Example systemd unit:

```ini
[Unit]
Description=OSE web application
After=network.target

[Service]
Type=simple
WorkingDirectory=/opt/ose
EnvironmentFile=/opt/ose/.env
Environment=NODE_ENV=production
ExecStart=/usr/bin/npm run start
Restart=always
RestartSec=5
User=ose
Group=ose

[Install]
WantedBy=multi-user.target
```

Enable it:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now ose
```

## Database

OSE currently ships with SQLite as the built-in supported database. The `DATABASE_URL` for all deployment modes should point to a SQLite file (e.g. `file:/data/ose.db`). PostgreSQL production deployment support is on the roadmap and is not available in the current version.

## HTTPS and Reverse Proxy

Example Nginx configuration:

```nginx
server {
  listen 80;
  server_name ose.example.com;
  return 301 https://$host$request_uri;
}

server {
  listen 443 ssl http2;
  server_name ose.example.com;

  ssl_certificate /etc/letsencrypt/live/ose.example.com/fullchain.pem;
  ssl_certificate_key /etc/letsencrypt/live/ose.example.com/privkey.pem;

  location / {
    proxy_pass http://127.0.0.1:3000;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto https;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  }
}
```

Set `NEXTAUTH_URL=https://ose.example.com` when using HTTPS.

## AI Model Defaults

Built-in default models are updated periodically as providers release new generations, but self-hosted operators can override defaults without waiting for a release.

Set any of these environment variables to pin a specific model for that provider:

```env
DEFAULT_CLAUDE_MODEL=claude-sonnet-4-6
DEFAULT_OPENAI_MODEL=gpt-5-mini
DEFAULT_GEMINI_MODEL=gemini-3-flash-preview
DEFAULT_CUSTOM_MODEL=default
```

Model selection follows this precedence order:

```
explicit request/body model > saved user setting > DEFAULT_<PROVIDER>_MODEL env > built-in fallback
```

This means a user's saved model preference always wins, and the env override only applies when no model has been saved by the user.

## Encryption Key (`OSE_ENCRYPTION_KEY`)

User-provided AI API keys (e.g. Anthropic, OpenAI, Gemini, custom provider) are encrypted at rest using AES-256-GCM before being written to the database. The `OSE_ENCRYPTION_KEY` environment variable supplies the 32-byte key.

**Generating the key:**

```bash
openssl rand -base64 32
```

**Behavior without the key:**

- Saving a new API key via the settings page returns HTTP 503 and the key is not persisted.
- Any legacy plaintext keys already in the database are treated as unconfigured until you set the key and they are migrated.

**Lazy migration of existing plaintext keys:**

If you upgrade from a deployment that stored keys in plaintext, existing `apiKey` / `imageApiKey` values are automatically encrypted on first use once `OSE_ENCRYPTION_KEY` is set. The plaintext column is cleared immediately after encryption. No manual migration step is required.

**Key rotation:**

1. Add a migration script that re-encrypts all stored keys with the new key.
2. Replace `OSE_ENCRYPTION_KEY` in your environment with the new value.
3. If you rotate without re-encrypting, users will need to re-enter their API keys.

**Backup:**

Back up `OSE_ENCRYPTION_KEY` separately from the database. Losing the key makes all stored user API keys permanently unreadable.
