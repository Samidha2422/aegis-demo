& (Join-Path $PSScriptRoot 'run-logged.ps1') -Recipe 'app-test'
exit $LASTEXITCODE
