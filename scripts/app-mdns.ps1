& (Join-Path $PSScriptRoot 'run-logged.ps1') -Recipe 'app-mdns'
exit $LASTEXITCODE
