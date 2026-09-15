#!/usr/bin/env bash
#
# What a pull request is told about the user interface: every TypeScript package is type-checked,
# and every Jest test of this repository is run. Nothing is bundled.
#
# A library is type-checked by building it. The 'tsup' configuration of each one emits type
# declarations, and emitting them is a full check of that package's sources.
#
# The packages depend on each other through a registry rather than through a workspace, so each
# one is published to the local registry as soon as it is built and the packages above it install
# what this branch produced. Without that step a package would be checked against the snapshot of
# 'main' which the lock file points at, and a change which splits two packages apart would still
# be green.
#
# What this does NOT do:
#
#  - it does not bundle the web application. The bundle is what a release needs, it is the slowest
#    part of the frontend build, and a type error does not wait for it.
#  - it does not build Storybook, the development shells or the simulator. They are examples,
#    nothing consumes them, and version 1.0 replaces them with a reference implementation (story
#    1238).
#  - it does not run a browser. Nothing here clicks through the application.
#
# Usage:  bin/frontend-checks.sh
#
# Needs node, npm and the local npm registry the build already uses - see development/README.md,
# which also explains the '.npmrc' this expects. Another registry goes in NPM_REGISTRY. The CI job
# 'frontend' starts Verdaccio from development/verdaccio and runs this script unchanged.
#
set -euo pipefail

cd "$(dirname "$0")/.."

export NPM_REGISTRY="${NPM_REGISTRY:-http://localhost:4873/}"

if ! curl --silent --fail "${NPM_REGISTRY%/}/-/ping" > /dev/null; then
  echo "No npm registry answers at ${NPM_REGISTRY}. development/README.md starts one." >&2
  exit 1
fi

# Two of the TypeScript clients are generated from OpenAPI documents by Maven rather than written,
# and both are git-ignored, so a fresh clone has neither and this check cannot make them.
if [ ! -f apis/official-gui-api/client/src/index.ts ] \
  || [ ! -f business-cockpit/src/main/webapp/src/client/gui/index.ts ]; then
  echo "The generated API clients are missing. Generate them first:" >&2
  echo "  mvn -Pjava-install -DskipTests -pl openapi-generator-fixes install" >&2
  echo "  mvn -Pjava-install -DskipTests -pl apis/official-gui-api/client,business-cockpit -am generate-sources" >&2
  exit 1
fi

# '--no-package-lock': the lock files of this repository resolve the @vanillabp packages to
# whichever registry the last committer used, and a check which rewrote them would leave a diff
# behind in every run.
npm_install() {
  npm install --no-package-lock --no-audit --no-fund --@vanillabp:registry="${NPM_REGISTRY}"
}

# Hands what was just built to the packages which depend on it. The version is a snapshot and the
# same one every time, so whatever a previous run left in the registry is taken back first - which
# is what the Maven profile 'unpublish-npm' does for a local build.
publish_to_the_local_registry() {
  npm run unpublish:snapshot > /dev/null 2>&1 || true
  npm publish --tag snapshot --@vanillabp:registry="${NPM_REGISTRY}" > /dev/null
}

check_library() {
  local package_dir="$1"
  echo "== ${package_dir}"
  (
    cd "$package_dir"
    npm_install
    npm run build
    if node -e "process.exit(require('./package.json').scripts.test ? 0 : 1)"; then
      npm test
    fi
    publish_to_the_local_registry
  )
}

check_library apis/official-gui-api/client
check_library ui/bc-types
check_library ui/bc-shared
check_library ui/bc-ui

# The web application is nobody's dependency, so it is not published and not bundled here. What is
# left is the whole-project type check and its own tests.
echo "== business-cockpit/src/main/webapp"
(
  cd business-cockpit/src/main/webapp
  npm_install
  npx tsc --noEmit
  CI=true npm test
)

echo
echo "The frontend type-checks and its tests are green."
