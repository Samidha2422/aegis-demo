$env:PATH = "C:\ProgramData\AndroidCLI;C:\Users\samid\AppData\Local\Android\Sdk\platform-tools;C:\Program Files\Eclipse Adoptium\jdk-17.0.13.11-hotspot\bin;" + $env:PATH
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.13.11-hotspot"
Push-Location android-app
try {
    if (Test-Path 'gradlew.bat') {
        cmd.exe /c "gradlew.bat assembleDebug > ..\build-logs\app-build.log 2>&1"
        $exitCode = $LASTEXITCODE
    } else {
        cmd.exe /c "android build > ..\build-logs\app-build.log 2>&1"
        $exitCode = $LASTEXITCODE
    }
} finally {
    Pop-Location
    Set-Content -Path build-logs/app-build.exit -Value $exitCode -Encoding ascii
    exit $exitCode
}
