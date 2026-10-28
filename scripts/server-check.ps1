$env:PATH = "C:\Users\samid\.cargo\bin;C:\Users\samid\.local\bin;" + $env:PATH
Push-Location chat-server
try {
    # 1. Run cargo fmt to format code cleanly
    cmd.exe /c "cargo fmt > ..\build-logs\server-check.log 2>&1"
    
    # 2. Run cargo clippy with -D warnings
    cmd.exe /c "cargo clippy -- -D warnings >> ..\build-logs\server-check.log 2>&1"
    $exitCode = $LASTEXITCODE

    # 3. If clippy passed, run cargo test
    if ($exitCode -eq 0) {
        cmd.exe /c "cargo test >> ..\build-logs\server-check.log 2>&1"
        $exitCode = $LASTEXITCODE
    }
} finally {
    Pop-Location
    Set-Content -Path build-logs/server-check.exit -Value $exitCode -Encoding ascii
    exit $exitCode
}
