$env:PATH = "C:\Users\samid\AppData\Local\Microsoft\WinGet\Packages\Google.PlatformTools_Microsoft.Winget.Source_8wekyb3d8bbwe\platform-tools;C:\ProgramData\AndroidCLI;C:\Users\samid\AppData\Local\Android\Sdk\platform-tools;" + $env:PATH
try {
    cmd.exe /c "adb mdns services > build-logs\app-mdns.log 2>&1"
    $exitCode = $LASTEXITCODE
} finally {
    Set-Content -Path build-logs\app-mdns.exit -Value $exitCode -Encoding ascii
    exit $exitCode
}
