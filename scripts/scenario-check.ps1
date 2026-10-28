& (Join-Path $PSScriptRoot 'run-logged.ps1') -Recipe 'scenario-check'
exit $LASTEXITCODE
