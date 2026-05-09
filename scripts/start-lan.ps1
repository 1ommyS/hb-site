$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$frontendDir = Join-Path $projectRoot "frontend"
$bun = Join-Path $env:USERPROFILE ".bun\bin\bun.exe"

if (-not (Test-Path $bun)) {
    throw "Bun not found at $bun"
}

$ip = $null

try {
    $ip = Get-NetIPAddress -AddressFamily IPv4 |
        Where-Object {
            $_.IPAddress -notlike "127.*" -and
            $_.IPAddress -notlike "169.254.*" -and
            $_.PrefixOrigin -ne "WellKnown"
        } |
        Sort-Object InterfaceMetric |
        Select-Object -First 1 -ExpandProperty IPAddress
} catch {
    $ip = $null
}

if (-not $ip) {
    $ip = [System.Net.Dns]::GetHostEntry([System.Net.Dns]::GetHostName()).AddressList |
        Where-Object {
            $_.AddressFamily -eq [System.Net.Sockets.AddressFamily]::InterNetwork -and
            $_.ToString() -notlike "127.*" -and
            $_.ToString() -notlike "169.254.*"
        } |
        Select-Object -First 1 |
        ForEach-Object { $_.ToString() }
}

if (-not $ip) {
    throw "Could not detect LAN IPv4 address. Check Wi-Fi/Ethernet connection."
}

Write-Host ""
Write-Host "Frontend LAN URL:" -ForegroundColor Cyan
Write-Host "  http://$ip`:5173/" -ForegroundColor Green
Write-Host ""
Write-Host "Open this URL on phones connected to the same Wi-Fi." -ForegroundColor Yellow
Write-Host "Backend must be running on this computer at http://localhost:8080." -ForegroundColor Yellow
Write-Host ""

Set-Location $frontendDir
& $bun run dev:lan
