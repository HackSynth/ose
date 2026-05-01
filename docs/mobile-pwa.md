# Mobile (PWA)

OSE ships a Progressive Web App (PWA) for Android and other modern mobile browsers. Users add a self-hosted OSE deployment to their home screen and run it like a native app — no app store needed.

## What is included

- `public/manifest.webmanifest` — app metadata (name, theme color, icons, display mode).
- `public/sw.js` — minimal service worker that caches static assets and provides a same-origin navigation fallback. API calls are never intercepted, so authentication and data stay fresh.
- `public/icons/icon-512.png` — 512×512 launcher icon (also used as the Apple touch icon).
- `<PWARegister />` in the root layout registers the service worker in production browser builds. It is a no-op inside the Tauri WebView and during `next dev`.

## Installing on Android

1. Deploy OSE to a server reachable over HTTPS (see [self-hosting.md](self-hosting.md)).
2. Open the deployment in Chrome, Edge, or Samsung Internet on Android.
3. Open the browser menu → **Add to Home screen** / **Install app**.
4. Launch OSE from the home screen — it runs in standalone mode without browser chrome.

> HTTPS is required for service worker registration. `http://localhost` works for testing but a public IP without TLS will not register the service worker.

## What is cached

| Resource                                            | Strategy                                    |
| --------------------------------------------------- | ------------------------------------------- |
| `/_next/static/*`                                   | Cache-first                                 |
| `/icons/*`, `/manifest.webmanifest`, `/favicon.ico` | Cache-first                                 |
| HTML navigations                                    | Network-first, cache fallback to last visit |
| `/api/*`                                            | Pass-through (never cached)                 |

The cache is versioned (`ose-shell-v1`). Bump `CACHE_NAME` in `public/sw.js` after a release that breaks the shell to force clients to re-fetch.

## Updating the service worker

Browsers check `sw.js` for updates on every navigation. The current implementation calls `self.skipWaiting()` and `clients.claim()` so an updated worker activates on the next reload — no extra UI needed for now. If you ship behavior that requires explicit user consent before updating (e.g. mid-session data migration), remove `skipWaiting` and gate it behind a `SKIP_WAITING` postMessage from the page.

## Why not Tauri Mobile?

Tauri Mobile (Android/iOS) is still a young surface, and OSE's Next.js + Prisma backend cannot be embedded into an Android APK in a reasonable size (Prisma engines are not built for Android). The current `OSE_MOBILE_URL` flow remains as a thin WebView shell for users who want an APK pointing at their own server, but PWA is the recommended mobile path.
