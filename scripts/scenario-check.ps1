$env:PATH = "C:\Users\samid\.cargo\bin;C:\Users\samid\.local\bin;" + $env:PATH
Push-Location chat-server
try {
    cmd.exe /c "cargo run --bin scenario_check > ..\build-logs\scenario-check.log 2>&1"
    $exitCode = $LASTEXITCODE
} finally {
    Pop-Location
    Set-Content -Path build-logs/scenario-check.exit -Value $exitCode -Encoding ascii
    exit $exitCode
}
