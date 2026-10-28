$env:PATH = "C:\ProgramData\AndroidCLI;C:\Users\samid\AppData\Local\Android\Sdk\platform-tools;" + $env:PATH
try {
    cmd.exe /c "android run --path android-app > build-logs\app-smoke.log 2>&1"
    Start-Sleep -Seconds 3
    cmd.exe /c "android screen capture --output build-logs\smoke_screen.png >> build-logs\app-smoke.log 2>&1"
    $exitCode = $LASTEXITCODE
} finally {
    Set-Content -Path build-logs/app-smoke.exit -Value $exitCode -Encoding ascii
    exit $exitCode
}
