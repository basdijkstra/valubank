# Starts every ValuBank service plus the frontend, each in its own PowerShell window.
# Run from anywhere; paths are resolved relative to this script's location.

$root = Split-Path -Parent $PSScriptRoot
if (-not $root) { $root = (Get-Item "$PSScriptRoot\..").FullName }

$services = @(
    @{ Name = "interest-rate-service"; Path = "$root\services\interest-rate-service" },
    @{ Name = "fraud-service";         Path = "$root\services\fraud-service" },
    @{ Name = "accounts-service";      Path = "$root\services\accounts-service" },
    @{ Name = "payments-service";      Path = "$root\services\payments-service" }
)

foreach ($svc in $services) {
    Write-Host "Starting $($svc.Name)..."
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$($svc.Path)'; mvn spring-boot:run" -WindowStyle Normal
}

Write-Host "Starting frontend..."
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root\frontend'; if (-not (Test-Path node_modules)) { npm install }; npm run dev" -WindowStyle Normal

Write-Host ""
Write-Host "All services launching in separate windows:"
Write-Host "  Interest Rate Service : http://localhost:8084"
Write-Host "  Fraud Service          : http://localhost:8083"
Write-Host "  Accounts Service       : http://localhost:8081"
Write-Host "  Payments Service       : http://localhost:8082"
Write-Host "  Frontend               : http://localhost:5173"
Write-Host ""
Write-Host "Close each window to stop that service."
