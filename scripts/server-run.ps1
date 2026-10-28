& (Join-Path $PSScriptRoot 'run-logged.ps1') -Recipe 'server-run'
exit $LASTEXITCODE
