#!/usr/bin/env node

const { execFileSync } = require('node:child_process');
const crypto = require('node:crypto');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const tauriDir = path.join(root, 'src-tauri');
const binariesDir = path.join(tauriDir, 'binaries');
const standaloneTarget = path.join(binariesDir, 'standalone');
const runtimeTarget = path.join(standaloneTarget, 'runtime');
const schemaPath = path.join(root, 'src', 'prisma', 'schema.prisma');

const BUNDLED_NODE_VERSION = process.env.BUNDLED_NODE_VERSION || 'v20.18.1';

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

function nodeArchiveDescriptor() {
  const platformMap = { win32: 'win', darwin: 'darwin', linux: 'linux' };
  const archMap = { x64: 'x64', arm64: 'arm64' };

  const platform = platformMap[process.platform];
  const arch = archMap[process.arch];
  if (!platform || !arch) {
    throw new Error(
      `Unsupported platform/arch combination for bundled Node.js: ${process.platform}/${process.arch}`
    );
  }

  const ext = platform === 'win' ? 'zip' : 'tar.xz';
  const baseName = `node-${BUNDLED_NODE_VERSION}-${platform}-${arch}`;
  return {
    baseName,
    fileName: `${baseName}.${ext}`,
    url: `https://nodejs.org/dist/${BUNDLED_NODE_VERSION}/${baseName}.${ext}`,
  };
}

async function downloadFile(url, destination) {
  fs.mkdirSync(path.dirname(destination), { recursive: true });
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Download failed (${response.status} ${response.statusText}): ${url}`);
  }
  const buffer = Buffer.from(await response.arrayBuffer());
  fs.writeFileSync(destination, buffer);
}

async function fetchExpectedSha256(fileName) {
  const url = `https://nodejs.org/dist/${BUNDLED_NODE_VERSION}/SHASUMS256.txt`;
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Failed to fetch SHASUMS256.txt (${response.status}): ${url}`);
  }
  const body = await response.text();
  for (const line of body.split('\n')) {
    const match = line.trim().match(/^([a-f0-9]{64})\s+(\S+)$/);
    if (match && match[2] === fileName) {
      return match[1];
    }
  }
  throw new Error(`No SHA-256 entry for ${fileName} in ${url}`);
}

function sha256OfFile(filePath) {
  const hash = crypto.createHash('sha256');
  hash.update(fs.readFileSync(filePath));
  return hash.digest('hex');
}

// On Windows, prefer the bundled bsdtar at System32\tar.exe.
// PATH-based `tar` may resolve to GNU tar (msys/Git Bash), which
// misinterprets paths like `C:\Users\...` as `host:path`.
function tarExecutable() {
  if (process.platform === 'win32') {
    const systemRoot = process.env.SystemRoot || 'C:\\Windows';
    const candidate = path.join(systemRoot, 'System32', 'tar.exe');
    if (fs.existsSync(candidate)) return candidate;
  }
  return 'tar';
}

async function bundleNodeRuntime() {
  const descriptor = nodeArchiveDescriptor();
  const cacheDir = path.join(os.tmpdir(), 'ose-node-cache');
  const archivePath = path.join(cacheDir, descriptor.fileName);

  console.log(`Verifying SHA-256 for ${descriptor.fileName} ...`);
  const expectedSha = await fetchExpectedSha256(descriptor.fileName);

  if (fs.existsSync(archivePath) && sha256OfFile(archivePath) === expectedSha) {
    console.log(`Using cached ${archivePath} (sha256 verified).`);
  } else {
    if (fs.existsSync(archivePath)) {
      console.log('Cached archive failed sha256 check; redownloading.');
      fs.rmSync(archivePath, { force: true });
    }
    console.log(`Downloading ${descriptor.url} ...`);
    await downloadFile(descriptor.url, archivePath);
    const actualSha = sha256OfFile(archivePath);
    if (actualSha !== expectedSha) {
      fs.rmSync(archivePath, { force: true });
      throw new Error(
        `SHA-256 mismatch for ${descriptor.fileName}: expected ${expectedSha}, got ${actualSha}`
      );
    }
    console.log('SHA-256 verified.');
  }

  const extractDir = path.join(cacheDir, `${descriptor.baseName}-extract`);
  fs.rmSync(extractDir, { recursive: true, force: true });
  fs.mkdirSync(extractDir, { recursive: true });

  console.log(`Extracting ${descriptor.fileName} ...`);
  // Both Windows 10+ (bsdtar via System32\tar.exe) and POSIX systems ship a
  // `tar` that handles .zip and .tar.xz natively.
  execFileSync(tarExecutable(), ['-xf', archivePath, '-C', extractDir], { stdio: 'inherit' });

  const extractedRoot = path.join(extractDir, descriptor.baseName);
  if (!fs.existsSync(extractedRoot)) {
    throw new Error(`Extraction did not produce expected directory: ${extractedRoot}`);
  }

  fs.rmSync(runtimeTarget, { recursive: true, force: true });
  fs.mkdirSync(runtimeTarget, { recursive: true });

  if (process.platform === 'win32') {
    fs.copyFileSync(path.join(extractedRoot, 'node.exe'), path.join(runtimeTarget, 'node.exe'));
  } else {
    const sourceBinary = path.join(extractedRoot, 'bin', 'node');
    const targetBinary = path.join(runtimeTarget, 'node');
    fs.copyFileSync(sourceBinary, targetBinary);
    fs.chmodSync(targetBinary, 0o755);
  }

  fs.writeFileSync(
    path.join(runtimeTarget, 'VERSION'),
    `${BUNDLED_NODE_VERSION}\n${process.platform}-${process.arch}\n`
  );

  console.log(
    `Bundled Node.js ${BUNDLED_NODE_VERSION} (${process.platform}-${process.arch}) into ${runtimeTarget}.`
  );
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
  fs.mkdirSync(standaloneTarget, { recursive: true });
  fs.writeFileSync(
    path.join(standaloneTarget, 'mobile-placeholder.txt'),
    'Android builds do not use the desktop Next.js sidecar.\n'
  );

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
  // Resolves the Node.js binary that should run the prisma CLI: prefer the bundled
  // runtime sibling to start.js, fall back to the parent process node (i.e. whatever
  // launched start.js — also our bundled binary when invoked via Tauri).
  const script = `const { spawnSync } = require("node:child_process");
const fs = require("node:fs");
const path = require("node:path");

process.env.PORT = process.env.PORT || "3000";
process.env.HOSTNAME = process.env.HOSTNAME || "127.0.0.1";

const bundledNode = process.platform === "win32"
  ? path.join(__dirname, "runtime", "node.exe")
  : path.join(__dirname, "runtime", "node");
const nodeBinary = fs.existsSync(bundledNode) ? bundledNode : process.execPath;

const prismaCli = path.join(__dirname, "node_modules", "prisma", "build", "index.js");
const prismaArgs = [
  prismaCli,
  "migrate",
  "deploy",
  "--schema",
  path.join(__dirname, "src", "prisma", "schema.prisma"),
];

const migrate = spawnSync(nodeBinary, prismaArgs, {
  cwd: __dirname,
  env: process.env,
  stdio: "inherit",
});

if (migrate.status !== 0) {
  process.exit(migrate.status || 1);
}

require("./server.js");
`;
  fs.writeFileSync(path.join(standaloneTarget, 'start.js'), script);
}

async function main() {
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
    await bundleNodeRuntime();
  } else {
    fs.rmSync(runtimeTarget, { recursive: true, force: true });
    console.log(
      'Using PATH Node.js mode. Install Node.js 20+ on target machines, or set BUNDLE_NODE=1 to ship a bundled runtime.'
    );
  }
}

if (require.main === module) {
  main().catch((error) => {
    console.error(error);
    process.exit(1);
  });
}

module.exports = {
  bundleNodeRuntime,
  nodeArchiveDescriptor,
  runtimeTarget,
  BUNDLED_NODE_VERSION,
};
