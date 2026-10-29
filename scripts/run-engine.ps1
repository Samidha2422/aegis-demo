$ErrorActionPreference = 'Stop'
Set-Location 'C:\Users\Vibhav\Documents\Projects\Adapt'
$env:HRE_SALT = 'replace-with-at-least-32-random-characters-secure-salt-1234'
$env:HRE_API_KEYS = 'demo-client-key'
$env:HRE_MODERATOR_TOKENS = 'demo-moderator-token'
$env:HRE_MODELS_DIR = 'models'
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/just.ps1 run
