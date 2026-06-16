#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ROOT/scripts/env.sh"
cd "$ROOT"
"$GRADLE_HOME/bin/gradle" -p ebb-llm-gateway --no-daemon gatewaySmoke
