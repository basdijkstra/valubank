# Stops whatever is listening on ValuBank's service ports. Safe to run any
# time - only touches these five ports, regardless of how the process was
# started (start-all.ps1, an IDE, manually, ...).

$ports = [ordered]@{
    8081 = "accounts-service"
    8082 = "payments-service"
    8083 = "fraud-service"
    8084 = "interest-rate-service"
    5173 = "frontend"
}

foreach ($port in $ports.Keys) {
    $name = $ports[$port]
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue

    if (-not $conns) {
        Write-Host "$name (:$port): nothing listening"
        continue
    }

    foreach ($conn in $conns | Select-Object -Unique OwningProcess) {
        $procId = $conn.OwningProcess
        try {
            $proc = Get-Process -Id $procId -ErrorAction Stop
            Write-Host "$name (:$port): stopping $($proc.ProcessName) (PID $procId)"
            Stop-Process -Id $procId -Force -ErrorAction Stop
        } catch {
            Write-Host "$name (:$port): could not stop PID $procId - $_"
        }
    }
}

Write-Host ""
Write-Host "Done. Leftover terminal windows (if any) can be closed manually."
