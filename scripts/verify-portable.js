#!/usr/bin/env node
// Sanity-check the BUNDLE_NODE=1 tauri:prepare output: required files, runnable
// node binary, and that prisma CLI is reachable. Does not start the full server
// (that is best validated via tauri:build + launching the installed app).

const { execFileSync } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const standalone = path.join(root, 'src-tauri', 'binaries', 'standalone');
const isWin = process.platform === 'win32';
const nodeBin = path.join(standalone, 'runtime', isWin ? 'node.exe' : 'node');

const required = [
  ['runtime node binary', nodeBin],
  ['start.js', path.join(standalone, 'start.js')],
  ['server.js', path.join(standalone, 'server.js')],
  ['prisma CLI', path.join(standalone, 'node_modules', 'prisma', 'build', 'index.js')],
  ['prisma schema', path.join(standalone, 'src', 'prisma', 'schema.prisma')],
  ['prisma migrations', path.join(standalone, 'src', 'prisma', 'migrations')],
  ['@prisma/client', path.join(standalone, 'node_modules', '@prisma', 'client')],
  ['.next/static', path.join(standalone, '.next', 'static')],
  ['public dir', path.join(standalone, 'public')],
];

let failed = 0;
for (const [label, target] of required) {
  if (fs.existsSync(target)) {
    const stats = fs.statSync(target);
    const size = stats.isFile() ? `${(stats.size / 1024).toFixed(0)}KB` : 'dir';
    console.log(`  OK   ${label.padEnd(22)} ${target} (${size})`);
  } else {
    console.log(`  MISS ${label.padEnd(22)} ${target}`);
    failed++;
  }
}

if (failed === 0) {
  console.log('\nRunning bundled node --version ...');
  const ver = execFileSync(nodeBin, ['--version'], { encoding: 'utf8' }).trim();
  console.log(`  bundled Node: ${ver}`);

  console.log('Running bundled node + prisma --version ...');
  const prismaVer = execFileSync(
    nodeBin,
    [path.join(standalone, 'node_modules', 'prisma', 'build', 'index.js'), '--version'],
    { encoding: 'utf8', cwd: standalone }
  ).trim();
  console.log(prismaVer.split('\n').map((l) => `  ${l}`).join('\n'));

  console.log('\nPortable bundle looks healthy.');
  process.exit(0);
}

console.error(`\n${failed} required artifact(s) missing — bundle is incomplete.`);
process.exit(1);
