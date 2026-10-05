# start-backend.ps1
# Run this script from the backend folder to cleanly start the Spring Boot server.
# It automatically kills any process already holding port 8081.

$port = 7070

Write-Host "Checking port $port..." -ForegroundColor Cyan

$existing = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
if ($existing) {
    $procId = $existing.OwningProcess
    Write-Host "Killing existing process on port $port (PID $procId)..." -ForegroundColor Yellow
    Stop-Process -Id $procId -Force
    Start-Sleep -Seconds 1
    Write-Host "Port $port is now free." -ForegroundColor Green
} else {
    Write-Host "Port $port is already free." -ForegroundColor Green
}

Write-Host ""
Write-Host "Starting Spring Boot backend..." -ForegroundColor Cyan
mvn spring-boot:run
