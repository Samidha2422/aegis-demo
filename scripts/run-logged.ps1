param([Parameter(Mandatory = $true)][string]$Recipe)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path $PSScriptRoot -Parent
$logDir = Join-Path $repoRoot 'build-logs'
New-Item -ItemType Directory -Path $logDir -Force | Out-Null
$logPath = Join-Path $logDir "$Recipe.log"
$exitPath = Join-Path $logDir "$Recipe.exit"
$exitCode = 1

function Resolve-Cargo {
    $command = Get-Command cargo -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    $candidate = Join-Path $env:USERPROFILE '.cargo\bin\cargo.exe'
    if (Test-Path -LiteralPath $candidate) { return $candidate }
    throw 'Cargo not found. Install Rust or add cargo to PATH.'
}

function Resolve-Android {
    $sdkCandidates = @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, (Join-Path $env:LOCALAPPDATA 'Android\Sdk'))
    foreach ($sdk in $sdkCandidates) {
        if ($sdk -and (Test-Path -LiteralPath (Join-Path $sdk 'platforms'))) {
            $env:ANDROID_HOME = $sdk
            $env:ANDROID_SDK_ROOT = $sdk
            $env:PATH = (Join-Path $sdk 'platform-tools') + ';' + $env:PATH
            break
        }
    }
    if (!(Test-Path -LiteralPath $env:ANDROID_HOME -ErrorAction SilentlyContinue)) { throw 'Set ANDROID_HOME to the installed Android SDK.' }
}

function Resolve-Java {
    $candidates = @($env:AEGIS_JAVA_HOME, $env:JAVA_HOME, "$env:ProgramFiles\Android\Android Studio\jbr")
    foreach ($ideRoot in @("$env:ProgramFiles\JetBrains", (Join-Path $env:LOCALAPPDATA 'Programs\RustRover'))) {
        if (Test-Path -LiteralPath $ideRoot) {
            $candidates += @(Get-ChildItem -LiteralPath $ideRoot -Directory | ForEach-Object {
                Join-Path $_.FullName 'jbr'
            })
        }
    }
    foreach ($vendor in @('Eclipse Adoptium', 'Microsoft', 'Java', 'Zulu', 'Android')) {
        $vendorRoot = Join-Path $env:ProgramFiles $vendor
        if (Test-Path -LiteralPath $vendorRoot) {
            $candidates += @(Get-ChildItem -LiteralPath $vendorRoot -Directory | ForEach-Object { $_.FullName })
        }
    }
    $javaCommand = Get-Command javac -ErrorAction SilentlyContinue
    if ($javaCommand) { $candidates += Split-Path (Split-Path $javaCommand.Source -Parent) -Parent }
    foreach ($jdk in $candidates) {
        if (!$jdk -or !(Test-Path -LiteralPath (Join-Path $jdk 'bin\javac.exe'))) { continue }
        $release = Join-Path $jdk 'release'
        if ((Test-Path -LiteralPath $release) -and ((Get-Content -LiteralPath $release -Raw) -match 'JAVA_VERSION="(17|21|25)[.\"]')) {
            $env:JAVA_HOME = $jdk
            $env:PATH = (Join-Path $jdk 'bin') + ';' + $env:PATH
            Write-Output "Using JDK: $jdk"
            return
        }
    }
    throw 'Set AEGIS_JAVA_HOME to an installed JDK 17, 21, or 25 (with javac). JDK 27 is not supported by Gradle 9.1.'
}

Push-Location $repoRoot
try {
    & {
        try {
            # Windows PowerShell treats native stderr as error records; keep those inside the log.
            $ErrorActionPreference = 'Continue'
            switch ($Recipe) {
                'server-check' {
                    $cargo = Resolve-Cargo
                    Push-Location chat-server
                    try {
                        & $cargo fmt --all -- --check
                        if ($LASTEXITCODE -ne 0) { $script:exitCode = $LASTEXITCODE; break }
                        & $cargo clippy --all-targets -- -D warnings
                        if ($LASTEXITCODE -ne 0) { $script:exitCode = $LASTEXITCODE; break }
                        & $cargo test --all-targets
                        $script:exitCode = $LASTEXITCODE
                    } finally { Pop-Location }
                }
                'server-format' {
                    $cargo = Resolve-Cargo
                    & $cargo fmt --manifest-path chat-server/Cargo.toml --all
                    $script:exitCode = $LASTEXITCODE
                }
                'server-run' {
                    $cargo = Resolve-Cargo
                    & $cargo run --manifest-path chat-server/Cargo.toml --bin chat-server
                    $script:exitCode = $LASTEXITCODE
                }
                'scenario-check' {
                    $cargo = Resolve-Cargo
                    Push-Location chat-server
                    try { & $cargo run --bin scenario_check; $script:exitCode = $LASTEXITCODE } finally { Pop-Location }
                }
                { $_ -in @('app-build', 'app-test') } {
                    Resolve-Android
                    Resolve-Java
                    Push-Location android-app
                    try {
                        $task = if ($Recipe -eq 'app-build') { 'assembleDebug' } else { 'testDebugUnitTest' }
                        & .\gradlew.bat --no-daemon $task
                        $script:exitCode = $LASTEXITCODE
                    } finally { Pop-Location }
                }
                { $_ -in @('app-install', 'app-smoke', 'app-devices', 'app-mdns', 'app-reverse') } {
                    Resolve-Android
                    $adb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
                    switch ($Recipe) {
                        'app-install' { & $adb install -r (Join-Path $repoRoot 'android-app\app\build\outputs\apk\debug\app-debug.apk') }
                        'app-smoke' { & $adb shell am start -W -n com.aegis.demo/.MainActivity }
                        'app-devices' { & $adb devices -l }
                        'app-mdns' { & $adb mdns services }
                        'app-reverse' { & $adb reverse tcp:8081 tcp:8081 }
                    }
                    $script:exitCode = $LASTEXITCODE
                }
                'integration-check' {
                    & (Join-Path $PSScriptRoot 'integration-check.ps1')
                    $script:exitCode = 0
                }
                'demo-reset' {
                    $url = if ($env:AEGIS_CHAT_URL) { $env:AEGIS_CHAT_URL.TrimEnd('/') } else { 'http://127.0.0.1:8081' }
                    Invoke-RestMethod -Method Post -Uri "$url/demo/reset" | Out-Null
                    $script:exitCode = 0
                }
                default { throw "Unknown logged recipe: $Recipe" }
            }
        } catch {
            Write-Output $_.Exception.Message
            $script:exitCode = 1
        }
    } *> $logPath
} finally {
    Pop-Location
    Set-Content -LiteralPath $exitPath -Value $exitCode -Encoding ascii
}
Write-Output "$Recipe finished (exit $exitCode); logs: build-logs/$Recipe.log"
exit $exitCode
