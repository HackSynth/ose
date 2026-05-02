# Desktop App

OSE uses Tauri for desktop packaging on Windows, Linux, and macOS.

## Prerequisites

- Node.js 20 (build host).
- npm.
- Rust toolchain.
- Tauri system dependencies for your platform.

Install Rust from https://rustup.rs/ and follow the Tauri prerequisites for Windows, macOS, or Linux.

## Development

```bash
npm install
npm run tauri:dev
```

Tauri runs the Next.js dev server and opens a desktop WebView.

## Production Build

```bash
npm run tauri:prepare
npm run tauri:build
```

`tauri:prepare` performs:

1. Prisma Client generation.
2. Next.js standalone build.
3. Copying `.next/standalone`, static assets, Prisma schema, migrations, and required Prisma packages into `src-tauri/binaries/standalone`.
4. Writing a `start.js` sidecar that runs `prisma migrate deploy` before `server.js`.

### Bundling Node.js (no system Node.js required at runtime)

By default the desktop installer relies on the user having Node.js 20+ on `PATH`. To produce a self-contained installer that ships its own Node.js:

```bash
BUNDLE_NODE=1 npm run tauri:prepare
npm run tauri:build
```

`BUNDLE_NODE=1` downloads the official Node.js binary for the build host's platform/architecture and places it under `src-tauri/binaries/standalone/runtime/`. The Tauri app prefers this binary over `PATH` at runtime. Override the version with `BUNDLED_NODE_VERSION=v20.18.1` (or any other Node.js dist tag) if needed.

> **Cross-compilation note**: the bundled Node.js matches the build host. To ship for a different OS/arch, run `tauri:prepare` on that platform (e.g. via CI matrix builds).

#### Caveats when shipping the bundled runtime

- **macOS code signing**: the official Node.js binary is signed by Node.js, but that signature is invalid once moved into another `.app` bundle. macOS Gatekeeper will quarantine an unsigned bundled `.app`. Configure a Developer ID identity in Tauri (`signingIdentity` / `entitlements`) before distributing to users. For local builds you can self-test with `xattr -cr <App>.app`, but never ask end users to do that.
- **Linux glibc compatibility**: the official `linux-x64.tar.xz` is built against glibc 2.28 (Debian 10 / Ubuntu 18 era). It will run on any modern distro but will fail on CentOS 7, RHEL 7, Ubuntu 16.04, or musl-only systems. Build on a similarly-aged base image (e.g. `debian:11`) if you need broader compatibility, or use a musl Node build.
- **Windows extraction**: `tauri:prepare` invokes `%SystemRoot%\System32\tar.exe` (bsdtar) explicitly to avoid GNU tar from Git Bash misinterpreting `C:\` paths as `host:path`. Windows 10 1803+ is required.
- **SHA-256 verification**: every download is hash-checked against the official `SHASUMS256.txt`. Cached archives that fail verification are deleted and re-downloaded.

## Runtime Behavior

In production desktop mode, Tauri:

1. Selects an available local port.
2. Creates an app data directory.
3. Sets `DATABASE_URL` to the local SQLite database.
4. Starts `node start.js` — preferring the bundled `runtime/node[.exe]` when present, otherwise `node` from `PATH`.
5. Polls `/api/ai/status` until the Next.js server is ready.
6. Navigates the WebView to `http://127.0.0.1:<port>`.

## Portable Zip (Windows)

The Windows portable zip is assembled by the release workflow from the Tauri build output. It is not a native Tauri installer target and has additional requirements compared to the MSI/NSIS installers.

### Required build flags

The portable zip must be built with `BUNDLE_NODE=1` so it ships its own Node.js runtime:

```bash
BUNDLE_NODE=1 npm run tauri:prepare
npm run tauri:build
```

Without `BUNDLE_NODE=1` the zip depends on `node` from the user's `PATH`, which may be absent, the wrong version, or blocked by system policy.

### Verifying the portable bundle

After `tauri:prepare`, run:

```bash
npm run verify:portable
```

This checks that all required artifacts are present (`runtime/node.exe`, `start.js`, `server.js`, Prisma engine, migrations, `.next/static`, `public`) and that the bundled Node.js is executable. The release workflow should fail if this check fails.

### Portable zip layout

```
OSE-Portable/
  OSE.exe
  portable.ini
  README.txt
  resources/
    binaries/
      standalone/
        runtime/
          node.exe        ← bundled Node.js (BUNDLE_NODE=1 required)
        start.js
        server.js
        .next/static/
        public/
        src/prisma/
        node_modules/
```

### Known limitations of the portable zip

- **App data is not portable.** OSE still writes to `%AppData%\com.ose.softwareexam` and the WebView data cache. Deleting the zip does not remove user data. Multiple extracted versions share the same data directory.
- **WebView2 is not bundled.** The app requires WebView2 Runtime (pre-installed on Windows 11 and updated Windows 10). On machines without it the window will fail to open. Document this in the release notes and consider linking to the WebView2 Evergreen installer.
- **No updater integration.** There is no automatic update path for the portable zip.
- **Path sensitivity.** Test extraction paths with spaces, non-ASCII characters, and paths longer than 260 characters (long path support must be enabled on older Windows).

## Troubleshooting

- **Server startup timeout**: verify Node.js is installed and available in `PATH`, or rebuild with `BUNDLE_NODE=1`. Check the startup log (see below).
- **Prisma errors**: run `npm run tauri:prepare` again and confirm migrations were copied.
- **Port issues**: OSE picks an unused port automatically.
- **Windows localhost issues**: the desktop app uses `127.0.0.1` to avoid IPv4/IPv6 mismatch.

### Startup log

On every launch the desktop app writes a `startup.log` file to the app data directory:

- **Windows**: `%AppData%\com.ose.softwareexam\startup.log`
- **macOS**: `~/Library/Application Support/com.ose.softwareexam/startup.log`
- **Linux**: `~/.local/share/com.ose.softwareexam/startup.log`

The log records the resolved standalone directory, Node.js binary path (bundled or system), selected port, database path, all sidecar stdout/stderr lines, and the final ready or timeout result. Share this file when reporting startup issues.
