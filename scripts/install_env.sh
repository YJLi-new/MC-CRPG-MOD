#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOOLS="$ROOT/.tools"
DOWNLOADS="$TOOLS/downloads"
mkdir -p "$DOWNLOADS"

JAVA_VERSION="25"
JDK_DIR="$TOOLS/jdk-25"
GRADLE_VERSION="9.5.1"
GRADLE_DIR="$TOOLS/gradle-$GRADLE_VERSION"

log() { printf '\n==> %s\n' "$*"; }

fetch_temurin_metadata() {
  "$ROOT/.tools_python_helper" 2>/dev/null || true
}

if [ ! -x "$JDK_DIR/bin/java" ] || [ ! -x "$JDK_DIR/bin/javac" ]; then
  log "Installing Eclipse Temurin JDK $JAVA_VERSION to $JDK_DIR"
  META_JSON="$DOWNLOADS/temurin-${JAVA_VERSION}-linux-x64-jdk.json"
  curl -fsSL --retry 3 --retry-delay 2 -A 'codex-crpg-mod-env-setup/1.0' \
    -o "$META_JSON" \
    "https://api.adoptium.net/v3/assets/latest/${JAVA_VERSION}/hotspot?architecture=x64&heap_size=normal&image_type=jdk&jvm_impl=hotspot&os=linux&vendor=eclipse"
  read -r JDK_URL EXPECTED_SHA JDK_NAME < <(python3 - <<PY
import json
with open('$META_JSON', encoding='utf-8') as f:
    data=json.load(f)
p=data[0]['binary']['package']
print(p['link'], p['checksum'], p['name'])
PY
)
  JDK_TGZ="$DOWNLOADS/$JDK_NAME"
  # Reuse old preflight download name if it exists and matches the expected checksum.
  OLD_TGZ="$ROOT/scripts/.tools/downloads/temurin-${JAVA_VERSION}-linux-x64-jdk.tar.gz"
  if [ ! -f "$JDK_TGZ" ] && [ -f "$OLD_TGZ" ]; then
    OLD_SHA="$(sha256sum "$OLD_TGZ" | awk '{print $1}')"
    if [ "$OLD_SHA" = "$EXPECTED_SHA" ]; then
      mv "$OLD_TGZ" "$JDK_TGZ"
    fi
  fi
  if [ ! -f "$JDK_TGZ" ]; then
    curl -fL --retry 3 --retry-delay 2 -A 'codex-crpg-mod-env-setup/1.0' -o "$JDK_TGZ" "$JDK_URL"
  fi
  ACTUAL_SHA="$(sha256sum "$JDK_TGZ" | awk '{print $1}')"
  if [ "$EXPECTED_SHA" != "$ACTUAL_SHA" ]; then
    echo "JDK checksum mismatch: expected $EXPECTED_SHA actual $ACTUAL_SHA" >&2
    exit 1
  fi
  rm -rf "$JDK_DIR.tmp" "$JDK_DIR"
  mkdir -p "$JDK_DIR.tmp"
  tar -xzf "$JDK_TGZ" -C "$JDK_DIR.tmp" --strip-components=1
  mv "$JDK_DIR.tmp" "$JDK_DIR"
else
  log "JDK already installed at $JDK_DIR"
fi

log "JDK version"
"$JDK_DIR/bin/java" -version
"$JDK_DIR/bin/javac" -version

if [ ! -x "$GRADLE_DIR/bin/gradle" ]; then
  log "Installing Gradle $GRADLE_VERSION to $GRADLE_DIR"
  GRADLE_ZIP="$DOWNLOADS/gradle-${GRADLE_VERSION}-bin.zip"
  if [ ! -f "$GRADLE_ZIP" ]; then
    curl -fL --retry 3 --retry-delay 2 -o "$GRADLE_ZIP" "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
  fi
  curl -fsSL -o "${GRADLE_ZIP}.sha256" "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip.sha256"
  EXPECTED="$(awk '{print $1}' "${GRADLE_ZIP}.sha256")"
  ACTUAL="$(sha256sum "$GRADLE_ZIP" | awk '{print $1}')"
  if [ "$EXPECTED" != "$ACTUAL" ]; then
    echo "Gradle checksum mismatch: expected $EXPECTED actual $ACTUAL" >&2
    exit 1
  fi
  rm -rf "$GRADLE_DIR.tmp" "$GRADLE_DIR"
  mkdir -p "$GRADLE_DIR.tmp"
  unzip -q "$GRADLE_ZIP" -d "$GRADLE_DIR.tmp"
  mv "$GRADLE_DIR.tmp/gradle-$GRADLE_VERSION" "$GRADLE_DIR"
  rmdir "$GRADLE_DIR.tmp"
else
  log "Gradle already installed at $GRADLE_DIR"
fi

log "Gradle version"
JAVA_HOME="$JDK_DIR" PATH="$JDK_DIR/bin:$GRADLE_DIR/bin:$PATH" "$GRADLE_DIR/bin/gradle" -v

log "Writing environment helper scripts"
mkdir -p "$ROOT/scripts"
cat > "$ROOT/scripts/env.sh" <<ENVSH
#!/usr/bin/env bash
# Source this file from CRPG_MOD to use the local JDK/Gradle toolchain.
export CRPG_MOD_ROOT="\$(cd "\$(dirname "\${BASH_SOURCE[0]}")/.." && pwd)"
export JAVA_HOME="\$CRPG_MOD_ROOT/.tools/jdk-25"
export GRADLE_HOME="\$CRPG_MOD_ROOT/.tools/gradle-$GRADLE_VERSION"
export GRADLE_USER_HOME="\$CRPG_MOD_ROOT/.gradle-user-home"
export PATH="\$JAVA_HOME/bin:\$GRADLE_HOME/bin:\$PATH"
ENVSH
chmod +x "$ROOT/scripts/env.sh"

cat > "$ROOT/scripts/env.ps1" <<ENVPS1
# Source this file from PowerShell to use project-local environment variables.
# Note: the installed JDK/Gradle binaries are Linux binaries for WSL builds.
\$Script:CrpgModRoot = Split-Path -Parent \$PSScriptRoot
\$env:CRPG_MOD_ROOT = \$Script:CrpgModRoot
\$env:JAVA_HOME = Join-Path \$Script:CrpgModRoot '.tools/jdk-25'
\$env:GRADLE_HOME = Join-Path \$Script:CrpgModRoot '.tools/gradle-$GRADLE_VERSION'
\$env:GRADLE_USER_HOME = Join-Path \$Script:CrpgModRoot '.gradle-user-home'
\$env:PATH = "\$env:JAVA_HOME/bin;\$env:GRADLE_HOME/bin;\$env:PATH"
ENVPS1

# Clean failed first-attempt misplaced cache if it only contains the bad duplicate checksum artifact.
rm -f "$ROOT/scripts/.tools/downloads/temurin-${JAVA_VERSION}-linux-x64-jdk.tar.gz.sha256.txt" 2>/dev/null || true
rmdir "$ROOT/scripts/.tools/downloads" "$ROOT/scripts/.tools" 2>/dev/null || true

log "Environment installed"
