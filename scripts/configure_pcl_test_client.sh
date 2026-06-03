#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MC_DIR="${MC_DIR:-/mnt/e/MC/PCL/.minecraft}"
BASE_ID="${BASE_ID:-26.1.2}"
PROFILE_ID="${PROFILE_ID:-26.1.2-Fabric-Ebb-Test}"
FABRIC_LOADER_VERSION="${FABRIC_LOADER_VERSION:-0.19.2}"
FABRIC_API_VERSION="${FABRIC_API_VERSION:-0.150.0+26.1.2}"
GECKOLIB_VERSION="${GECKOLIB_VERSION:-5.5.1}"

BASE_DIR="$MC_DIR/versions/$BASE_ID"
PROFILE_DIR="$MC_DIR/versions/$PROFILE_ID"
LIB_DIR="$MC_DIR/libraries"
DOWNLOAD_DIR="$PROJECT_DIR/.tools/downloads"
FABRIC_PROFILE_JSON="$DOWNLOAD_DIR/fabric-loader-$BASE_ID-$FABRIC_LOADER_VERSION.profile.json"

require_file() {
  if [[ ! -f "$1" ]]; then
    echo "Missing required file: $1" >&2
    exit 1
  fi
}

find_one() {
  local pattern="$1"
  local found
  found="$(find "$PROJECT_DIR/.gradle-user-home/caches/modules-2/files-2.1" -path "$pattern" -type f | head -n 1 || true)"
  if [[ -z "$found" ]]; then
    echo "Could not find dependency in Gradle cache: $pattern" >&2
    exit 1
  fi
  printf '%s\n' "$found"
}

mkdir -p "$DOWNLOAD_DIR"
require_file "$BASE_DIR/$BASE_ID.json"
require_file "$BASE_DIR/$BASE_ID.jar"
require_file "$PROJECT_DIR/build/libs/ebb-0.1.0-dev.jar"

profile_running_pids() {
  if ! command -v powershell.exe >/dev/null 2>&1; then
    return 0
  fi
  PROFILE_ID="$PROFILE_ID" powershell.exe -NoProfile -Command '
$profile = $env:PROFILE_ID
Get-CimInstance Win32_Process |
  Where-Object {
    ($_.Name -eq "javaw.exe" -or $_.Name -eq "java.exe") -and
    $_.CommandLine -and
    ($_.CommandLine.Contains("--version $profile") -or $_.CommandLine.Contains("versions\$profile"))
  } |
  ForEach-Object { $_.ProcessId }
' 2>/dev/null | tr -d '\r'
}

RUNNING_PROFILE_PIDS="$(profile_running_pids | sed '/^$/d' || true)"
if [[ -n "$RUNNING_PROFILE_PIDS" && "${EBB_ALLOW_RUNNING_PROFILE_REFRESH:-}" != "1" ]]; then
  cat >&2 <<EOF_RUNNING
Refusing to refresh $PROFILE_ID while its Minecraft Java process is still running.
Running PID(s): $RUNNING_PROFILE_PIDS

Reason: overwriting profile-local mod jars while Minecraft is open can leave the JVM
with stale ZIP offsets and later produce errors such as:
  ZipFile invalid LOC header / Failed to load class file

Fully close Minecraft first, then rerun this script. If you are intentionally doing
low-level testing, set EBB_ALLOW_RUNNING_PROFILE_REFRESH=1 to override.
EOF_RUNNING
  exit 2
fi

if [[ ! -f "$FABRIC_PROFILE_JSON" ]]; then
  curl -fsSL "https://meta.fabricmc.net/v2/versions/loader/$BASE_ID/$FABRIC_LOADER_VERSION/profile/json" -o "$FABRIC_PROFILE_JSON"
fi

mkdir -p "$PROFILE_DIR/PCL" "$PROFILE_DIR/mods" "$LIB_DIR"

backup_if_present() {
  local file="$1"
  if [[ -f "$file" ]]; then
    cp -a "$file" "$file.bak.$(date +%Y%m%d-%H%M%S)"
  fi
}
backup_if_present "$PROFILE_DIR/$PROFILE_ID.json"
backup_if_present "$PROFILE_DIR/PCL/Setup.ini"

python3 - <<'PY' "$BASE_DIR/$BASE_ID.json" "$FABRIC_PROFILE_JSON" "$PROFILE_DIR/$PROFILE_ID.json" "$PROFILE_ID" "$BASE_ID" "$FABRIC_LOADER_VERSION"
import json, sys
from pathlib import Path
base_path, fabric_path, out_path, profile_id, base_id, loader_version = map(Path, sys.argv[1:7])
profile_id = str(profile_id)
base_id = str(base_id)
loader_version = str(loader_version)
base = json.loads(base_path.read_text(encoding='utf-8'))
fabric = json.loads(fabric_path.read_text(encoding='utf-8'))
merged = dict(base)
merged['id'] = profile_id
merged['clientVersion'] = base_id
merged['mainClass'] = fabric['mainClass']
merged['type'] = 'release'
merged['releaseTime'] = fabric.get('releaseTime', base.get('releaseTime'))
merged['time'] = fabric.get('time', base.get('time'))
# Full PCL-style profile: include vanilla libraries plus Fabric loader libraries.
libs = []
seen = set()
for lib in base.get('libraries', []) + fabric.get('libraries', []):
    name = lib.get('name')
    if name and name in seen:
        continue
    if name:
        seen.add(name)
    libs.append(lib)
merged['libraries'] = libs
args = dict(base.get('arguments', {}))
args['jvm'] = list(args.get('jvm', [])) + list(fabric.get('arguments', {}).get('jvm', []))
args['game'] = list(args.get('game', [])) + list(fabric.get('arguments', {}).get('game', []))
merged['arguments'] = args
# Use a profile-local jar copied from the vanilla 26.1.2 jar; no inherited vanilla profile mutation.
merged.pop('inheritsFrom', None)
merged.pop('minecraftArguments', None)
out_path.write_text(json.dumps(merged, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
PY

cp -a "$BASE_DIR/$BASE_ID.jar" "$PROFILE_DIR/$PROFILE_ID.jar"
if [[ -f "$BASE_DIR/options.txt" && ! -f "$PROFILE_DIR/options.txt" ]]; then
  cp -a "$BASE_DIR/options.txt" "$PROFILE_DIR/options.txt"
fi
if [[ -d "$BASE_DIR/$BASE_ID-natives" && ! -d "$PROFILE_DIR/$PROFILE_ID-natives" ]]; then
  cp -a "$BASE_DIR/$BASE_ID-natives" "$PROFILE_DIR/$PROFILE_ID-natives"
fi

# Copy Fabric loader libraries into PCL's Minecraft library cache so the profile is immediately runnable.
python3 - <<'PY' "$FABRIC_PROFILE_JSON" "$LIB_DIR" "$PROJECT_DIR"
import hashlib, json, shutil, subprocess, sys
from pathlib import Path
profile = json.loads(Path(sys.argv[1]).read_text(encoding='utf-8'))
lib_dir = Path(sys.argv[2])
project = Path(sys.argv[3])
gradle_cache = project / '.gradle-user-home/caches/modules-2/files-2.1'

def maven_path(coord: str) -> str:
    parts = coord.split(':')
    group, artifact, version = parts[:3]
    classifier = parts[3] if len(parts) > 3 else None
    filename = f"{artifact}-{version}" + (f"-{classifier}" if classifier else '') + '.jar'
    return '/'.join(group.split('.') + [artifact, version, filename])

def find_cache(coord: str) -> Path | None:
    rel = maven_path(coord)
    filename = rel.split('/')[-1]
    matches = list(gradle_cache.glob(f"**/{filename}"))
    return matches[0] if matches else None

def verify(path: Path, lib: dict):
    for algo in ('sha256', 'sha1', 'md5'):
        expected = lib.get(algo)
        if expected:
            h = hashlib.new(algo)
            h.update(path.read_bytes())
            actual = h.hexdigest()
            if actual.lower() != expected.lower():
                raise SystemExit(f"Checksum mismatch for {path}: {algo} {actual} != {expected}")
            return

for lib in profile.get('libraries', []):
    coord = lib['name']
    rel = Path(maven_path(coord))
    dest = lib_dir / rel
    if dest.exists():
        verify(dest, lib)
        continue
    src = find_cache(coord)
    dest.parent.mkdir(parents=True, exist_ok=True)
    if src is not None:
        shutil.copy2(src, dest)
    else:
        base_url = lib.get('url', 'https://maven.fabricmc.net/')
        if not base_url.endswith('/'):
            base_url += '/'
        subprocess.run(['curl', '-fsSL', base_url + rel.as_posix(), '-o', str(dest)], check=True)
    verify(dest, lib)
    print(f"installed library {coord} -> {dest}")
PY

FABRIC_API_JAR="$(find_one "*net.fabricmc.fabric-api/fabric-api/$FABRIC_API_VERSION/*/fabric-api-$FABRIC_API_VERSION.jar")"
GECKOLIB_JAR="$(find_one "*com.geckolib/geckolib-fabric-$BASE_ID/$GECKOLIB_VERSION/*/geckolib-fabric-$BASE_ID-$GECKOLIB_VERSION.jar")"
cp -a "$PROJECT_DIR/build/libs/ebb-0.1.0-dev.jar" "$PROFILE_DIR/mods/ebb-0.1.0-dev.jar"
cp -a "$FABRIC_API_JAR" "$PROFILE_DIR/mods/fabric-api-$FABRIC_API_VERSION.jar"
cp -a "$GECKOLIB_JAR" "$PROFILE_DIR/mods/geckolib-fabric-$BASE_ID-$GECKOLIB_VERSION.jar"

cat > "$PROFILE_DIR/PCL/Setup.ini" <<EOF_INI
VersionArgumentIndie:1
VersionArgumentIndieV2:True
State:9
Info:$BASE_ID, Fabric $FABRIC_LOADER_VERSION, Ebb CRPG test
Logo:pack://application:,,,/Plain Craft Launcher 2;component/Images/Blocks/Grass.png
ReleaseTime:2026-05-30 15:00
VersionFabric:$FABRIC_LOADER_VERSION
VersionOptiFine:
VersionLiteLoader:False
VersionForge:
VersionNeoForge:
VersionApiCode:1902
VersionOriginal:$BASE_ID
VersionOriginalMain:26
VersionOriginalSub:1
VersionAdvanceJvm:
EOF_INI

cat > "$PROFILE_DIR/PCL/ebb-test-manifest.txt" <<EOF_MANIFEST
Profile: $PROFILE_ID
Minecraft dir: $MC_DIR
Base version: $BASE_ID
Fabric Loader: $FABRIC_LOADER_VERSION
Fabric API: $FABRIC_API_VERSION
GeckoLib: $GECKOLIB_VERSION
Mod jar: ebb-0.1.0-dev.jar
Configured at: $(date -Iseconds)
EOF_MANIFEST

cat > "$PROFILE_DIR/command_history.txt" <<'EOF_COMMANDS'
/ebb status
/ebb data
/ebb dev
/ebb journal
/ebb quest
/ebb dialogue vars
/ebb vars
/ebb attributes
/ebb attributes spend charisma 1
/ebb attributes grant 8
/ebb summon_npc demo/innkeeper_day
/ebb summon_npc demo/witness_day
/ebb summon_npc demo/tenant_day
/ebb summon_npc demo/guard_day
/setblock 0 64 4 minecraft:oak_door[half=lower,facing=south]
/setblock 0 65 4 minecraft:oak_door[half=upper,facing=south]
/setblock 2 64 1 minecraft:lectern
/setblock 1 65 0 minecraft:oak_sign[facing=south]
/setblock -2 64 6 minecraft:glass
/setblock 4 65 6 minecraft:gray_wool
/setblock 8 64 3 minecraft:chest[facing=south]
/setblock 6 63 7 minecraft:acacia_fence
/setblock 10 64 5 minecraft:oak_door[half=lower,facing=south]
/setblock 10 65 5 minecraft:oak_door[half=upper,facing=south]
/tag @e[type=minecraft:villager,limit=1,sort=nearest] add ebb.npc.innkeeper
EOF_COMMANDS

printf 'Configured playable PCL test client profile:\n  %s\n' "$PROFILE_DIR"
printf 'Mods installed:\n'; ls -1 "$PROFILE_DIR/mods" | sed 's/^/  - /'
printf 'Version JSON:\n  %s\n' "$PROFILE_DIR/$PROFILE_ID.json"
