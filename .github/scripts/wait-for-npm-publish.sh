#!/usr/bin/env bash
# Wait until the package just published from the current directory is queryable
# on the npm registry. Workaround for CDN replication lag that otherwise breaks
# subsequent `npm install`/`npm update` calls of dependent packages in the same
# Maven build.
#
# Invoked from each published @vanillabp/* package.json — see its `postpublish`
# (or for Angular, the end of `publish:release` / `publish:snapshot`). npm runs
# lifecycle hooks with the package directory as CWD, so package.json here is
# always the one that was just published.
#
# Gated by the WAIT_FOR_NPM_PUBLISH env var (set only in .github/workflows/release.yml).
# The package.json hook already checks the var before invoking this script; the
# check is repeated here as a second line of defence in case someone runs it
# directly outside CI.
set -e

if [ "${WAIT_FOR_NPM_PUBLISH:-0}" != "1" ]; then
  exit 0
fi

if [ ! -f package.json ]; then
  echo "wait-for-npm-publish: no package.json in $(pwd), skipping" >&2
  exit 0
fi

name=$(node -p "require('./package.json').name || ''" 2>/dev/null || echo "")
version=$(node -p "require('./package.json').version || ''" 2>/dev/null || echo "")
if [ -z "$name" ] || [ -z "$version" ]; then
  echo "wait-for-npm-publish: missing name/version in $(pwd)/package.json, skipping" >&2
  exit 0
fi

echo ">>> Waiting for ${name}@${version} to be queryable on the npm registry..."
for i in $(seq 1 60); do
  if npm view "${name}@${version}" version >/dev/null 2>&1; then
    echo ">>> ${name}@${version} available after attempt ${i}"
    exit 0
  fi
  sleep 5
done

echo "::error::${name}@${version} did not appear on the registry within 5 minutes — subsequent installs will fail"
exit 1
