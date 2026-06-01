#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

scripts/gradle-local.sh --no-daemon build
scripts/compile_authoring_sources.py --clean
CP="$(cat build/tmp/runtime-classpath.txt)"
mkdir -p build/tmp/smoke-classes
.tools/jdk-25/bin/javac -cp "$CP" -d build/tmp/smoke-classes scripts/smoke/*.java
for class_file in scripts/smoke/*.java; do
  class_name="$(basename "$class_file" .java)"
  .tools/jdk-25/bin/java -cp "build/tmp/smoke-classes:$CP" "$class_name"
done

if [ -d build/tmp/verify-src ]; then
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
