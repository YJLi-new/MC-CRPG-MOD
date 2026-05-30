# Source this file from PowerShell to use project-local environment variables.
# Note: the installed JDK/Gradle binaries are Linux binaries for WSL builds.
$Script:CrpgModRoot = Split-Path -Parent $PSScriptRoot
$env:CRPG_MOD_ROOT = $Script:CrpgModRoot
$env:JAVA_HOME = Join-Path $Script:CrpgModRoot '.tools/jdk-25'
$env:GRADLE_HOME = Join-Path $Script:CrpgModRoot '.tools/gradle-9.5.1'
$env:GRADLE_USER_HOME = Join-Path $Script:CrpgModRoot '.gradle-user-home'
$env:PATH = "$env:JAVA_HOME/bin;$env:GRADLE_HOME/bin;$env:PATH"
