# Contributing

## Scope
- This repository is primarily maintained as a single-owner project for OSE.
- Contributions should stay aligned with the existing product scope: single-user, local-first, low-maintenance.

## Setup
- Copy environment variables: `cp .env.example .env`
- Start stack: `docker compose up -d --build`
- Frontend test: `cd frontend && npm run test -- --run`
- Frontend build: `cd frontend && npm run build`
- Backend test: `docker compose run --rm backend-test`
- E2E test: `docker compose --profile tools run --rm e2e-test`

## Change Guidelines
- Prefer small, focused changes.
- Do not introduce external paid dependencies for core flows.
- Keep Chinese-first UX and stable `data-testid` selectors for major UI flows.
- Update README and `docs/delivery-report.md` when behavior changes.

## Pull Requests
- Describe the user-facing change.
- List verification commands you ran.
- Call out known limitations or follow-up work if any.
