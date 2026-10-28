& (Join-Path $PSScriptRoot 'run-logged.ps1') -Recipe 'server-check'
exit $LASTEXITCODE
