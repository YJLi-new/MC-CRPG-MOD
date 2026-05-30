#!/usr/bin/env bash
# Source this file from CRPG_MOD to use the local JDK/Gradle toolchain.
export CRPG_MOD_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export JAVA_HOME="$CRPG_MOD_ROOT/.tools/jdk-25"
export GRADLE_HOME="$CRPG_MOD_ROOT/.tools/gradle-9.5.1"
export GRADLE_USER_HOME="$CRPG_MOD_ROOT/.gradle-user-home"
export PATH="$JAVA_HOME/bin:$GRADLE_HOME/bin:$PATH"
