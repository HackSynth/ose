# Security Policy

## Supported Versions

OSE is pre-1.0 software. Security fixes are applied to the `main` branch and the latest tagged release when practical.

| Version | Supported |
| ------- | --------- |
| 0.1.x   | Yes       |

## Reporting a Vulnerability

Please do not open a public GitHub issue for security vulnerabilities.

Email the maintainers at `security@ose.dev` with:

- A clear description of the issue.
- Steps to reproduce or a proof of concept.
- Impact assessment, including affected routes or deployment modes.
- Your contact information and preferred disclosure timeline.

If `security@ose.dev` is not yet active, use a private GitHub security advisory or contact the repository owner directly.

## Response Process

1. We acknowledge reports within 72 hours when possible.
2. We validate the issue and estimate severity.
3. We prepare a fix, test it, and coordinate disclosure.
4. We publish release notes and credit the reporter unless anonymity is requested.

## Scope

In scope:

- Authentication and authorization bugs.
- API routes that leak user data.
- Injection, SSRF, XSS, CSRF, and unsafe file handling.
- AI provider key leakage or insecure storage.
- Docker, Tauri, and deployment configuration vulnerabilities.

Out of scope:

- Social engineering.
- Vulnerabilities in unsupported forks.
- Denial-of-service reports without a practical mitigation path.
