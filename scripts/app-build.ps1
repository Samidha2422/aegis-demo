& (Join-Path $PSScriptRoot 'run-logged.ps1') -Recipe 'app-build'
exit $LASTEXITCODE
