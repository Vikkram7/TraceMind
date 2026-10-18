# TraceMind - Local Standalone Startup Script
# Starts Backend Engine, Microservices, and Frontend Console

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " TraceMind — Observability & Root-Cause Analysis Platform" -ForegroundColor Cyan
Write-Host " Tagline: Every failure has a story. Find its origin." -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Start TraceMind Backend (Port 8080)
Write-Host "[1/5] Starting TraceMind Backend Engine on http://localhost:8080..." -ForegroundColor Green
$backendProc = Start-Process -FilePath "mvn.cmd" -ArgumentList "spring-boot:run" -WorkingDirectory "$PSScriptRoot\backend" -PassThru

# 2. Start Order Service (Port 8081)
Write-Host "[2/5] Starting Order Microservice on http://localhost:8081..." -ForegroundColor Green
$orderProc = Start-Process -FilePath "mvn.cmd" -ArgumentList "spring-boot:run" -WorkingDirectory "$PSScriptRoot\services\order-service" -PassThru

# 3. Start Payment Service (Port 8082)
Write-Host "[3/5] Starting Payment Microservice on http://localhost:8082..." -ForegroundColor Green
$paymentProc = Start-Process -FilePath "mvn.cmd" -ArgumentList "spring-boot:run" -WorkingDirectory "$PSScriptRoot\services\payment-service" -PassThru

# 4. Start Inventory Service (Port 8083)
Write-Host "[4/5] Starting Inventory Microservice on http://localhost:8083..." -ForegroundColor Green
$inventoryProc = Start-Process -FilePath "mvn.cmd" -ArgumentList "spring-boot:run" -WorkingDirectory "$PSScriptRoot\services\inventory-service" -PassThru

# 5. Start Frontend Console (Port 5173)
Write-Host "[5/5] Starting TraceMind Web Console on http://localhost:5173..." -ForegroundColor Green
$frontendProc = Start-Process -FilePath "cmd.exe" -ArgumentList "/c npm.cmd run dev" -WorkingDirectory "$PSScriptRoot\frontend" -PassThru

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " TraceMind is now running!" -ForegroundColor Green
Write-Host " Web Console:        http://localhost:5173" -ForegroundColor Yellow
Write-Host " Backend API Docs:   http://localhost:8080/swagger-ui.html" -ForegroundColor Yellow
Write-Host " Order Service:      http://localhost:8081" -ForegroundColor Yellow
Write-Host " Payment Service:    http://localhost:8082" -ForegroundColor Yellow
Write-Host " Inventory Service:  http://localhost:8083" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "To stop all services, run: .\stop-all.ps1" -ForegroundColor Cyan
