#!/usr/bin/env node

const { execFileSync } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const tauriDir = path.join(root, 'src-tauri');
const binariesDir = path.join(tauriDir, 'binaries');
const standaloneTarget = path.join(binariesDir, 'standalone');
const schemaPath = path.join(root, 'src', 'prisma', 'schema.prisma');

function run(command, args, options = {}) {
  console.log(`> ${command} ${args.join(' ')}`);
  execFileSync(command, args, {
    cwd: root,
    stdio: 'inherit',
    shell: process.platform === 'win32',
    env: { ...process.env, TAURI_BUILD: '1', ...options.env },
  });
}

function copyDir(source, target) {
  if (!fs.existsSync(source)) return;
  fs.rmSync(target, { recursive: true, force: true });
  fs.cpSync(source, target, { recursive: true });
}

function copyFile(source, target) {
  if (!fs.existsSync(source)) return;
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.copyFileSync(source, target);
}

function packageDir(packageName) {
  if (packageName.startsWith('@')) {
    const [scope, name] = packageName.split('/');
    return path.join(root, 'node_modules', scope, name);
  }

  return path.join(root, 'node_modules', packageName);
}

function copyPackageClosure(packageNames) {
  const seen = new Set();
  const queue = [...packageNames];

  while (queue.length > 0) {
    const packageName = queue.shift();
    if (seen.has(packageName)) continue;
    seen.add(packageName);

    const source = packageDir(packageName);
    const packageJsonPath = path.join(source, 'package.json');
    if (!fs.existsSync(packageJsonPath)) continue;

    copyDir(source, path.join(standaloneTarget, 'node_modules', ...packageName.split('/')));

    const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
    for (const deps of [
      packageJson.dependencies,
      packageJson.optionalDependencies,
      packageJson.peerDependencies,
    ]) {
      for (const dependencyName of Object.keys(deps ?? {})) {
        queue.push(dependencyName);
      }
    }
  }
}

function detectTargetTriple() {
  const archMap = { x64: 'x86_64', arm64: 'aarch64' };
  const arch = archMap[process.arch] ?? process.arch;
  if (process.platform === 'win32') return `${arch}-pc-windows-msvc`;
  if (process.platform === 'darwin') return `${arch}-apple-darwin`;
  if (process.platform === 'linux') return `${arch}-unknown-linux-gnu`;
  return `${arch}-${process.platform}`;
}

function writeFrontendPlaceholder() {
  const outDir = path.join(root, 'out');
  fs.mkdirSync(outDir, { recursive: true });
  fs.writeFileSync(
    path.join(outDir, 'index.html'),
    '<!doctype html><html><head><meta charset="utf-8"><title>OSE</title></head><body><p>Starting OSE...</p></body></html>'
  );
}

function writeAndroidWebviewEntry() {
  const outDir = path.join(root, 'out');
  const mobileUrl = process.env.OSE_MOBILE_URL || process.env.NEXT_PUBLIC_OSE_MOBILE_URL || '';
  fs.mkdirSync(outDir, { recursive: true });

  if (mobileUrl) {
    fs.writeFileSync(
      path.join(outDir, 'index.html'),
      `<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover"><title>OSE</title><script>location.replace(${JSON.stringify(mobileUrl)});</script></head><body><p>Opening OSE...</p><p><a href=${JSON.stringify(mobileUrl)}>Continue</a></p></body></html>`
    );
    return;
  }

  fs.writeFileSync(
    path.join(outDir, 'index.html'),
    `<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover"><title>OSE</title><style>body{margin:0;font-family:system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;background:#f8fafc;color:#0f172a}main{min-height:100vh;box-sizing:border-box;padding:32px 22px;display:flex;flex-direction:column;justify-content:center;gap:16px}h1{font-size:24px;line-height:1.2;margin:0}p{font-size:15px;line-height:1.6;margin:0;color:#475569}code{font-family:ui-monospace,SFMono-Regular,Menlo,monospace;background:#e2e8f0;border-radius:6px;padding:2px 6px;color:#0f172a}</style></head><body><main><h1>OSE mobile server is not configured</h1><p>The Android APK is a WebView client. It cannot run the bundled Next.js, Prisma, and Node.js desktop server inside Android.</p><p>Set <code>OSE_MOBILE_URL</code> or <code>NEXT_PUBLIC_OSE_MOBILE_URL</code> in the build environment to an HTTPS OSE deployment, then rebuild the APK.</p></main></body></html>`
  );
}

function writeStartScript() {
  const script = `const { spawnSync } = require("node:child_process");
const path = require("node:path");

process.env.PORT = process.env.PORT || "3000";
process.env.HOSTNAME = process.env.HOSTNAME || "127.0.0.1";

const prismaBin = process.platform === "win32"
  ? path.join(__dirname, "node_modules", ".bin", "prisma.cmd")
  : path.join(__dirname, "node_modules", ".bin", "prisma");

const migrate = spawnSync(prismaBin, ["migrate", "deploy", "--schema", path.join(__dirname, "src", "prisma", "schema.prisma")], {
  cwd: __dirname,
  env: process.env,
  stdio: "inherit",
  shell: process.platform === "win32",
});

if (migrate.status !== 0) {
  process.exit(migrate.status || 1);
}

require("./server.js");
`;
  fs.writeFileSync(path.join(standaloneTarget, 'start.js'), script);
}

function main() {
  if (process.env.TAURI_MOBILE_BUILD === '1') {
    writeAndroidWebviewEntry();
    console.log('Prepared OSE Android WebView entry.');
    return;
  }

  fs.mkdirSync(binariesDir, { recursive: true });

  run('npx', ['prisma', 'generate', '--schema', schemaPath]);
  run('npx', ['next', 'build']);

  const standaloneSource = path.join(root, '.next', 'standalone');
  if (!fs.existsSync(standaloneSource)) {
    throw new Error(".next/standalone not found. Check next.config output: 'standalone'.");
  }

  fs.rmSync(standaloneTarget, { recursive: true, force: true });
  copyDir(standaloneSource, standaloneTarget);
  copyDir(path.join(root, '.next', 'static'), path.join(standaloneTarget, '.next', 'static'));
  copyDir(path.join(root, 'public'), path.join(standaloneTarget, 'public'));
  copyDir(
    path.join(root, 'src', 'prisma', 'migrations'),
    path.join(standaloneTarget, 'src', 'prisma', 'migrations')
  );
  copyFile(schemaPath, path.join(standaloneTarget, 'src', 'prisma', 'schema.prisma'));
  copyDir(
    path.join(root, 'node_modules', '@prisma'),
    path.join(standaloneTarget, 'node_modules', '@prisma')
  );
  copyPackageClosure(['prisma']);
  copyFile(
    path.join(root, 'node_modules', '.bin', 'prisma'),
    path.join(standaloneTarget, 'node_modules', '.bin', 'prisma')
  );
  copyFile(
    path.join(root, 'node_modules', '.bin', 'prisma.cmd'),
    path.join(standaloneTarget, 'node_modules', '.bin', 'prisma.cmd')
  );
  writeStartScript();
  writeFrontendPlaceholder();

  const triple = detectTargetTriple();
  console.log(`Prepared OSE sidecar for ${triple}.`);

  if (process.env.BUNDLE_NODE === '1') {
    console.warn(
      'BUNDLE_NODE=1 is reserved for full runtime bundling. Current script uses PATH Node.js mode by default.'
    );
  } else {
    console.log('Using PATH Node.js mode. Install Node.js 20+ on target machines.');
  }
}

main();
