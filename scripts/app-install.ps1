& (Join-Path $PSScriptRoot 'run-logged.ps1') -Recipe 'app-install'
exit $LASTEXITCODE
