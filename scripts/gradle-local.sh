#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ROOT/scripts/env.sh"
cd "$ROOT"
# Prefer the installed project-local Gradle distribution to avoid wrapper network downloads.
exec "$GRADLE_HOME/bin/gradle" "$@"
