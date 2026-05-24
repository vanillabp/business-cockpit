#!/usr/bin/env node
// Generic wrapper: publish from a sub-directory and optionally wait for the
// registry to converge.
//
// Needed because `npm run X -- arg` appends `arg` to the *end* of the whole
// script string. So `cd subdir && npm publish && other-cmd` would forward
// the extra args to `other-cmd`, not to `npm publish` — which silently breaks
// the registry override the maven frontend plugin passes in.
//
// Usage (from a package.json script):
//   node <repo-root>/.github/scripts/publish-and-wait.js <publish-dir> [npm-publish-args...]
// where `npm run <script> -- <extra-args>` forwards <extra-args> to npm publish.
const cp = require('child_process');
const path = require('path');

const [, , dir, ...publishArgs] = process.argv;
if (!dir) {
  console.error('Usage: publish-and-wait.js <publish-dir> [npm-publish-args...]');
  process.exit(1);
}

const quoted = publishArgs
  .map(a => `'${a.replace(/'/g, "'\\''")}'`)
  .join(' ');
cp.execSync(`npm publish ${quoted}`, { stdio: 'inherit', cwd: dir });

if (process.env.WAIT_FOR_NPM_PUBLISH === '1') {
  const root = process.env.GITHUB_WORKSPACE
    || cp.execSync('git rev-parse --show-toplevel').toString().trim();
  const wait = path.join(root, '.github', 'scripts', 'wait-for-npm-publish.sh');
  cp.execSync(`bash "${wait}"`, { stdio: 'inherit', cwd: dir });
}
