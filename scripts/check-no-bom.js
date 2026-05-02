'use strict';

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const BOM = Buffer.from([0xef, 0xbb, 0xbf]);
const fixMode = process.argv.includes('--fix');
const explicitFiles = process.argv.slice(2).filter(a => !a.startsWith('--'));

function getTrackedFiles() {
  return execSync('git ls-files', { encoding: 'utf8' })
    .trim()
    .split('\n')
    .filter(Boolean);
}

function hasBom(filePath) {
  try {
    const buf = fs.readFileSync(filePath);
    return buf.length >= 3 && buf[0] === 0xef && buf[1] === 0xbb && buf[2] === 0xbf;
  } catch {
    return false;
  }
}

function stripBom(filePath) {
  const buf = fs.readFileSync(filePath);
  fs.writeFileSync(filePath, buf.slice(3));
}

const files = explicitFiles.length > 0 ? explicitFiles : getTrackedFiles();
const bomFiles = files.filter(f => fs.existsSync(f) && hasBom(f));

if (bomFiles.length === 0) {
  console.log('No BOM found.');
  process.exit(0);
}

if (fixMode) {
  bomFiles.forEach(f => {
    stripBom(f);
    console.log('Fixed:', f);
  });
  console.log(`Removed BOM from ${bomFiles.length} file(s).`);
  process.exit(0);
} else {
  bomFiles.forEach(f => console.error('BOM detected:', f));
  console.error(`\n${bomFiles.length} file(s) contain UTF-8 BOM. Run with --fix to remove them.`);
  process.exit(1);
}
