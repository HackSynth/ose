# Configuration

OSE is configured through environment variables.

## Database

- `DATABASE_URL`: Prisma connection string.
  - Development SQLite: `file:./dev.db`
  - Docker SQLite: `file:/data/ose.db`
  - Production PostgreSQL: `postgresql://user:password@host:5432/ose`

## Authentication

- `NEXTAUTH_SECRET`: required for secure sessions. Generate with `openssl rand -base64 32`.
- `NEXTAUTH_URL`: canonical application URL. Use `http://localhost:3000` locally and your HTTPS domain in production.
- `AUTH_SECRET` and `AUTH_URL`: compatibility variables for Auth.js deployments. Prefer `NEXTAUTH_*` when documenting new installs.

## AI Provider Selection

- `AI_PROVIDER`: optional. Values: `claude`, `openai`, `gemini`, `custom`.
- If empty, OSE auto-detects based on available keys.

## Claude

- `ANTHROPIC_API_KEY`: Claude key.
- `ANTHROPIC_BASE_URL`: optional proxy or compatible endpoint.
- `ANTHROPIC_MODEL`: optional model override.

## OpenAI

- `OPENAI_API_KEY`: OpenAI key.
- `OPENAI_BASE_URL`: optional, defaults to OpenAI API.
- `OPENAI_MODEL`: optional model override.

## Gemini

- `GEMINI_API_KEY`: Google AI Studio key.
- `GEMINI_MODEL`: optional model override.

## Custom OpenAI-compatible Endpoint

- `CUSTOM_API_KEY`: key for the compatible endpoint.
- `CUSTOM_BASE_URL`: required for custom mode.
- `CUSTOM_MODEL`: model name.

Examples include Ollama, DeepSeek, Qwen, vLLM, LM Studio, LocalAI, and Azure OpenAI-compatible gateways.

## Exam Settings

- `NEXT_PUBLIC_EXAM_DATE`: optional target exam date displayed by the UI.

## Desktop Runtime

The Tauri desktop app starts a bundled Next.js standalone server. It sets `PORT`, `HOSTNAME`, `DATABASE_URL`, `AUTH_URL`, and `NEXTAUTH_URL` at runtime.

## Android Runtime

- `OSE_MOBILE_URL`: HTTPS URL of a deployed OSE web service for the Android APK WebView.
- `NEXT_PUBLIC_OSE_MOBILE_URL`: fallback name for the same value.

The Android APK does not run the desktop Node.js/Prisma sidecar inside Android. Configure `OSE_MOBILE_URL` as a GitHub Actions repository variable or secret before building release APKs.
