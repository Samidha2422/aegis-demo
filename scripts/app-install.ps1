$env:PATH = "C:\ProgramData\AndroidCLI;C:\Users\samid\AppData\Local\Android\Sdk\platform-tools;" + $env:PATH
$apkPath = "$PSScriptRoot\..\android-app\app\build\outputs\apk\debug\app-debug.apk"

try {
    cmd.exe /c "adb install -r -d -g `"$apkPath`" > build-logs\app-install.log 2>&1"
    $exitCode = $LASTEXITCODE
} finally {
    Set-Content -Path build-logs\app-install.exit -Value $exitCode -Encoding ascii
    exit $exitCode
}
