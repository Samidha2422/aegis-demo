$env:PATH = "C:\ProgramData\AndroidCLI;C:\Users\samid\AppData\Local\Android\Sdk\platform-tools;" + $env:PATH
try {
    cmd.exe /c "adb shell am start -n com.aegis.demo/.MainActivity > build-logs\app-smoke.log 2>&1"
    Start-Sleep -Seconds 3
    cmd.exe /c "adb exec-out screencap -p > build-logs\smoke_screen.png 2>&1"
    $exitCode = $LASTEXITCODE
} finally {
    Set-Content -Path build-logs\app-smoke.exit -Value $exitCode -Encoding ascii
    exit $exitCode
}
