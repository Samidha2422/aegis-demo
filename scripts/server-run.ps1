$env:PATH = "C:\Users\samid\.cargo\bin;C:\Users\samid\.local\bin;" + $env:PATH
Push-Location chat-server
try {
    cmd.exe /c "cargo run > ..\build-logs\server-run.log 2>&1"
} finally {
    Pop-Location
}
