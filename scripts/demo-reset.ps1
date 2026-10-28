try {
    curl.exe -s -X POST http://localhost:8080/demo/reset *>&1 | Out-File -FilePath build-logs/demo-reset.log -Encoding ascii
    $exitCode = $LASTEXITCODE
} finally {
    Set-Content -Path build-logs/demo-reset.exit -Value $exitCode -Encoding ascii
    exit $exitCode
}
