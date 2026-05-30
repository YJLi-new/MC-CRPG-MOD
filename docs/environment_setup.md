# Local Development Environment

Installed under this `CRPG_MOD` workspace:

- Eclipse Temurin JDK 25: `.tools/jdk-25`
- Gradle 9.5.1: `.tools/gradle-9.5.1`
- Project-local Gradle cache: `.gradle-user-home`

Recommended WSL/bash usage:

```bash
scripts/gradle-local.sh --version
scripts/gradle-local.sh --no-daemon build
```

Alternative manual environment setup:

```bash
source scripts/env.sh
gradle --version
gradle --no-daemon build
```

`./gradlew` is present for standard Gradle-wrapper workflows, but the local helper is preferred in this workspace because it uses the already-installed `.tools/gradle-9.5.1` distribution and avoids extra wrapper downloads.

Pinned Minecraft/Fabric libraries are in `gradle.properties`:

- Minecraft `26.1.2`
- Fabric Loader `0.19.2`
- Fabric API `0.150.0+26.1.2`
- Fabric Loom `1.17.0-alpha.13`
- GeckoLib Fabric `26.1.2` line, version `5.5.1`

The PCL test client at `../.minecraft/versions/26.1.2` is currently vanilla; do not copy built jars into the client until an actual client test is requested.
