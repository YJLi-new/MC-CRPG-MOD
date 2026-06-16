#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

scripts/gradle-local.sh --no-daemon build
scripts/p36_gateway_smoke.sh
scripts/p37_gateway_chat_smoke.sh
scripts/p38_memory_smoke.sh
scripts/p39_memory_consolidation_smoke.sh
scripts/compile_authoring_sources.py --clean
scripts/compile_authoring_sources.py --source authoring/examples/tavern_case --out build/generated/ebb_authoring_examples/tavern_case/data/ebb --clean
scripts/p24_authoring_validation.py
scripts/p24_authoring_validation.py build/generated/ebb_authoring_examples/tavern_case/data/ebb
bad_authoring_dir="$(mktemp -d)"
trap 'rm -rf "$bad_authoring_dir"' EXIT
mkdir -p "$bad_authoring_dir/dialogues"
printf 'id: bad_dialogue\nnodes:\n  start: [\n' > "$bad_authoring_dir/dialogues/bad.yaml"
if scripts/compile_authoring_sources.py --source "$bad_authoring_dir" --out build/tmp/bad_authoring_diag --clean >"$bad_authoring_dir/stdout.txt" 2>"$bad_authoring_dir/stderr.txt"; then
  echo "Expected malformed authoring YAML to fail" >&2
  exit 1
fi
grep -Eq 'bad.yaml:[0-9]+:[0-9]+: invalid YAML' "$bad_authoring_dir/stderr.txt"
CP="$(cat build/tmp/runtime-classpath.txt)"
mkdir -p build/tmp/smoke-classes
.tools/jdk-25/bin/javac -cp "$CP" -d build/tmp/smoke-classes scripts/smoke/*.java
for class_file in scripts/smoke/*.java; do
  class_name="$(basename "$class_file" .java)"
  .tools/jdk-25/bin/java -cp "build/tmp/smoke-classes:$CP" "$class_name"
done

if [ "${EBB_RUN_LEGACY_VERIFY_SRC:-0}" = "1" ] && [ -d build/tmp/verify-src ]; then
  mkdir -p build/tmp/verify
  .tools/jdk-25/bin/javac -cp "$CP" -d build/tmp/verify build/tmp/verify-src/*.java
  for class_file in build/tmp/verify-src/*.java; do
    class_name="$(basename "$class_file" .java)"
    .tools/jdk-25/bin/java -cp "build/tmp/verify:$CP" "$class_name"
  done
fi
scripts/third_review_static_audit.py
scripts/deep_research_static_audit.py
scripts/goal_static_audit.py
scripts/gui_retest_issue_audit.py --skip-profile
