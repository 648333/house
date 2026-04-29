$ErrorActionPreference = "SilentlyContinue"

function Stop-ProcessByPort {
    param([int]$Port)

    $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    foreach ($connection in $connections) {
        if ($connection.OwningProcess -and $connection.OwningProcess -ne 0) {
            Stop-Process -Id $connection.OwningProcess -Force -ErrorAction SilentlyContinue
        }
    }
}

Write-Host ""
Write-Host "=== Warm Home Demo Stopper ===" -ForegroundColor Cyan
Write-Host "Stopping demo services..." -ForegroundColor Yellow

Stop-ProcessByPort -Port 5173
Stop-ProcessByPort -Port 8080

Write-Host "Frontend and backend stop commands have been sent." -ForegroundColor Green
Write-Host ""
