$ErrorActionPreference = 'Stop'
$chatUrl = if ($env:AEGIS_CHAT_URL) { $env:AEGIS_CHAT_URL.TrimEnd('/') } else { 'http://127.0.0.1:8081' }
$engineUrl = if ($env:AEGIS_ENGINE_URL) { $env:AEGIS_ENGINE_URL.TrimEnd('/') } else { 'http://127.0.0.1:8080' }
if (!$env:AEGIS_ENGINE_API_KEY) { throw 'Set AEGIS_ENGINE_API_KEY for the live engine comparison.' }
$headers = @{ 'X-API-Key' = $env:AEGIS_ENGINE_API_KEY }
$status = Invoke-RestMethod -Uri "$chatUrl/engine/status"
if ($status.health.mock) { throw 'Expected a real Adapt engine; mock mode is active.' }
if (!$status.models) { throw 'Engine returned no model health entries.' }
$before = Invoke-RestMethod -Uri "$chatUrl/chats/chat_rohan/messages"
$evidenceBefore = Invoke-RestMethod -Uri "$chatUrl/evidence/export?format=json"
$draft = 'I will kill you'
$nudgeBody = @{ draft = $draft; chat_id = 'chat_rohan'; sender_id = 'meera' } | ConvertTo-Json
$nudge = Invoke-RestMethod -Method Post -Uri "$chatUrl/nudge" -ContentType 'application/json' -Body $nudgeBody
$directBody = @{
    event_id = [guid]::NewGuid().ToString(); kind = 'message'; occurred_at = [DateTime]::UtcNow.ToString('o'); draft = $true
    sender_id = 'meera'; recipient_id = 'rohan'; platform = 'demo-chat'
    channel = @{ type = 'dm'; conversation_id = 'chat_rohan'; post_id = $null }
    context = @{ is_contact = $false; sender_account_age_days = 365 }
    message = @{ text = $draft }
} | ConvertTo-Json -Depth 8
$direct = Invoke-RestMethod -Method Post -Uri "$engineUrl/v1/analyze" -Headers $headers -ContentType 'application/json' -Body $directBody
if ($nudge.severity -ne $direct.severity -or $nudge.tier -ne $direct.tier -or $nudge.recommended_action -ne $direct.recommended_action) { throw 'Chat nudge differs from the real engine verdict.' }
$after = Invoke-RestMethod -Uri "$chatUrl/chats/chat_rohan/messages"
$evidenceAfter = Invoke-RestMethod -Uri "$chatUrl/evidence/export?format=json"
if (@($before).Count -ne @($after).Count) { throw 'Draft check changed chat history.' }
if (@($evidenceBefore.records).Count -ne @($evidenceAfter.records).Count) { throw 'Draft check persisted an engine event.' }
Write-Output 'PASS: live status and draft verdict match Adapt; drafts changed neither chat nor engine evidence.'
# This final request deliberately creates one committed, benign engine/chat event.
$message = Invoke-RestMethod -Method Post -Uri "$chatUrl/chats/chat_rohan/messages" -ContentType 'application/json' -Body '{"text":"Hello, hope your day is going well","sender_id":"meera"}'
if ($message.safety.state -eq 'EngineUnavailable' -or !$message.safety.modules) { throw 'Committed message was not scored by Adapt.' }
[guid]$parsedId = [guid]::Empty
if (![guid]::TryParse($message.id, [ref]$parsedId)) { throw 'Message ID is not an engine-compatible UUID.' }
Write-Output 'PASS: committed message received a real engine verdict with module metadata.'
