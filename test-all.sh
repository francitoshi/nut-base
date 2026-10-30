#!/usr/bin/env bash
set -euo pipefail

# Runs every module's test suite in dependency order.
# The order follows the `api project(':...')` declarations in each module's
# build.gradle: a module only runs after all the modules it depends on.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

MODULES=(
    nut-base-commons
    nut-base-collections
    nut-base-math
    nut-base-net
    nut-base-io
    nut-base-crypto
    nut-base-text
    nut-base-ui
    nut-base-util
    nut-base-audio
)

for module in "${MODULES[@]}"
do
    echo
    echo "==================================================================="
    echo "Running tests: $module"
    echo "==================================================================="
    cd "$ROOT_DIR/$module"
    "$ROOT_DIR/gradlew" --configure-on-demand --rerun-tasks -x check test
done

echo
echo "All module tests passed."
