#!/usr/bin/env node

const { execFileSync } = require('node:child_process');

execFileSync('npx', ['tauri', 'android', 'build', '--apk'], {
  cwd: process.cwd(),
  env: { ...process.env, TAURI_MOBILE_BUILD: '1' },
  stdio: 'inherit',
  shell: process.platform === 'win32',
});
