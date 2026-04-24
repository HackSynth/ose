#!/usr/bin/env node

const { execFileSync } = require("node:child_process");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const tauriDir = path.join(root, "src-tauri");
const binariesDir = path.join(tauriDir, "binaries");
const standaloneTarget = path.join(binariesDir, "standalone");
const schemaPath = path.join(root, "src", "prisma", "schema.prisma");

function run(command, args, options = {}) {
  console.log(`> ${command} ${args.join(" ")}`);
  execFileSync(command, args, {
    cwd: root,
    stdio: "inherit",
    shell: process.platform === "win32",
    env: { ...process.env, TAURI_BUILD: "1", ...options.env },
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

function detectTargetTriple() {
  const archMap = { x64: "x86_64", arm64: "aarch64" };
  const arch = archMap[process.arch] ?? process.arch;
  if (process.platform === "win32") return `${arch}-pc-windows-msvc`;
  if (process.platform === "darwin") return `${arch}-apple-darwin`;
  if (process.platform === "linux") return `${arch}-unknown-linux-gnu`;
  return `${arch}-${process.platform}`;
}

function writeFrontendPlaceholder() {
  const outDir = path.join(root, "out");
  fs.mkdirSync(outDir, { recursive: true });
  fs.writeFileSync(
    path.join(outDir, "index.html"),
    "<!doctype html><html><head><meta charset=\"utf-8\"><title>OSE</title></head><body><p>Starting OSE...</p></body></html>"
  );
}

function writeStartScript() {
  const script = `const { spawnSync } = require("node:child_process");
const path = require("node:path");

process.env.PORT = process.env.PORT || "3000";
process.env.HOSTNAME = process.env.HOSTNAME || "localhost";

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
  fs.writeFileSync(path.join(standaloneTarget, "start.js"), script);
}

function main() {
  fs.mkdirSync(binariesDir, { recursive: true });

  run("npx", ["prisma", "generate", "--schema", schemaPath]);
  run("npx", ["next", "build"]);

  const standaloneSource = path.join(root, ".next", "standalone");
  if (!fs.existsSync(standaloneSource)) {
    throw new Error(".next/standalone not found. Check next.config output: 'standalone'.");
  }

  fs.rmSync(standaloneTarget, { recursive: true, force: true });
  copyDir(standaloneSource, standaloneTarget);
  copyDir(path.join(root, ".next", "static"), path.join(standaloneTarget, ".next", "static"));
  copyDir(path.join(root, "public"), path.join(standaloneTarget, "public"));
  copyDir(path.join(root, "src", "prisma", "migrations"), path.join(standaloneTarget, "src", "prisma", "migrations"));
  copyFile(schemaPath, path.join(standaloneTarget, "src", "prisma", "schema.prisma"));
  copyDir(path.join(root, "node_modules", "prisma"), path.join(standaloneTarget, "node_modules", "prisma"));
  copyDir(path.join(root, "node_modules", "@prisma"), path.join(standaloneTarget, "node_modules", "@prisma"));
  copyFile(path.join(root, "node_modules", ".bin", "prisma"), path.join(standaloneTarget, "node_modules", ".bin", "prisma"));
  copyFile(path.join(root, "node_modules", ".bin", "prisma.cmd"), path.join(standaloneTarget, "node_modules", ".bin", "prisma.cmd"));
  writeStartScript();
  writeFrontendPlaceholder();

  const triple = detectTargetTriple();
  console.log(`Prepared OSE sidecar for ${triple}.`);

  if (process.env.BUNDLE_NODE === "1") {
    console.warn("BUNDLE_NODE=1 is reserved for full runtime bundling. Current script uses PATH Node.js mode by default.");
  } else {
    console.log("Using PATH Node.js mode. Install Node.js 20+ on target machines.");
  }
}

main();
