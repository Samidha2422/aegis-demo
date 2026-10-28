& (Join-Path $PSScriptRoot 'run-logged.ps1') -Recipe 'app-smoke'
exit $LASTEXITCODE
