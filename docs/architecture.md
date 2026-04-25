# Architecture

OSE is a Next.js application with Prisma-backed persistence, AI provider adapters, and an optional Tauri desktop shell.

## High-level Diagram

```text
Browser / Tauri WebView
        |
        v
Next.js App Router
  - Server components
  - Client components
  - API routes
        |
        +--> Auth.js / NextAuth session layer
        |
        +--> Prisma Client --> SQLite or PostgreSQL
        |
        +--> AI Provider Abstraction
               |--> Claude
               |--> OpenAI
               |--> Gemini
               |--> Custom OpenAI-compatible endpoint
```

## Frontend

Pages live under `src/app`. Interactive workflows such as practice sessions, exams, AI actions, and settings use client components in `src/components`.

## Backend API

API routes live under `src/app/api`. They validate sessions, parse request payloads, call domain logic in `src/lib`, and return JSON responses.

## Domain Logic

`src/lib` contains reusable logic for:

- Authentication and validation.
- Practice sessions and grading.
- Exam generation and scoring.
- Learning analytics.
- AI provider selection, prompts, rate limiting, and JSON parsing.

## Database

Prisma schema, migrations, and seed data live under `src/prisma`. SQLite is the default development database. PostgreSQL is recommended for production.

## Desktop App

The Tauri app lives in `src-tauri`. Production desktop builds bundle a Next.js standalone output and start it as a local server. The WebView navigates to the local server URL.

## Deployment Shapes

- Local development: Next.js dev server + SQLite.
- Docker: standalone Next.js server + SQLite volume or PostgreSQL.
- VPS: Node.js process managed by systemd.
- Desktop: Tauri shell + local standalone server + local SQLite database.
