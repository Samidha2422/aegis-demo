$env:PATH = "C:\ProgramData\AndroidCLI;C:\Users\samid\AppData\Local\Android\Sdk\platform-tools;" + $env:PATH
try {
    cmd.exe /c "android install --path android-app > build-logs\app-install.log 2>&1"
    $exitCode = $LASTEXITCODE
} finally {
    Set-Content -Path build-logs/app-install.exit -Value $exitCode -Encoding ascii
    exit $exitCode
}
