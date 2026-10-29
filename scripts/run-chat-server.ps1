$ErrorActionPreference = 'Stop'
Set-Location 'C:\Users\Vibhav\Documents\Projects\aegis-demo'
$env:AEGIS_ENGINE_URL = 'http://127.0.0.1:8080'
$env:AEGIS_ENGINE_API_KEY = 'demo-client-key'
$env:PORT = '8081'
cargo run --manifest-path chat-server/Cargo.toml --bin chat-server
