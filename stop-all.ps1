# TraceMind - Stop Script
# Terminates background Java microservices and node dev servers

Write-Host "Stopping TraceMind services..." -ForegroundColor Yellow

Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object { 
    $_.MainWindowTitle -like "*tracemind*" -or $_.CommandLine -like "*tracemind*" -or $_.CommandLine -like "*order-service*" 
} | Stop-Process -Force -ErrorAction SilentlyContinue

Get-Process -Name "node" -ErrorAction SilentlyContinue | Where-Object {
    $_.CommandLine -like "*vite*"
} | Stop-Process -Force -ErrorAction SilentlyContinue

Write-Host "TraceMind processes terminated." -ForegroundColor Green
