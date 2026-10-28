# Justfile for Aegis Demo (Luna Protocol Compliant)
# Every recipe outputs to build-logs/<recipe>.log and writes the exit code to build-logs/<recipe>.exit

set shell := ["powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command"]

default:
    @just --list

_ensure-logs:
    @if (!(Test-Path "build-logs")) { New-Item -ItemType Directory -Path "build-logs" -Force | Out-Null }

# Chat Server Quality Gate (fmt, clippy, test)
server-check: _ensure-logs
    @powershell.exe -ExecutionPolicy Bypass -File scripts/server-check.ps1

# Run chat-server in background or console
server-run: _ensure-logs
    @powershell.exe -ExecutionPolicy Bypass -File scripts/server-run.ps1

# Android App Build
app-build: _ensure-logs
    @powershell.exe -ExecutionPolicy Bypass -File scripts/app-build.ps1

# Android App Tests
app-test: _ensure-logs
    @powershell.exe -ExecutionPolicy Bypass -File scripts/app-test.ps1

# Android App Install on Device/Emulator
app-install: _ensure-logs
    @powershell.exe -ExecutionPolicy Bypass -File scripts/app-install.ps1

# Android App Smoke Test (Launch & Screen Capture)
app-smoke: _ensure-logs
    @powershell.exe -ExecutionPolicy Bypass -File scripts/app-smoke.ps1

# Scenario Self-Check
scenario-check: _ensure-logs
    @powershell.exe -ExecutionPolicy Bypass -File scripts/scenario-check.ps1

# Reset Demo State
demo-reset: _ensure-logs
    @powershell.exe -ExecutionPolicy Bypass -File scripts/demo-reset.ps1
