# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to semantic versioning where practical.

## [0.1.0] - 2026-04-25

### Added

- Initial OSE web application for Software Designer exam preparation.
- Multiple-choice practice, case analysis practice, wrong-note review, and mock exam flows.
- Knowledge point hierarchy with learning analytics, heatmaps, weak-area diagnosis, predicted score, and pass probability.
- AI features for explanation, grading, question generation, diagnosis, chat, and personalized study planning.
- Support for Claude, OpenAI, Gemini, and custom OpenAI-compatible endpoints.
- User authentication, profile management, password reset flow, and per-user AI settings.
- Tauri desktop packaging with local Next.js standalone server startup.
- Docker and GitHub Actions scaffolding for community development and deployment.

### Fixed

- Desktop service startup health checks now use IPv4 loopback consistently on Windows.

### Security

- Project license changed to AGPL-3.0 to protect community contributions in hosted deployments.
