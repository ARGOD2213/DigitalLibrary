param()

$ErrorActionPreference = "Stop"
$IP   = "13.233.106.4"
$KEY  = "$PSScriptRoot\digital-library-key.pem"
$SSH  = "ssh -i `"$KEY`" -o StrictHostKeyChecking=no -o ConnectTimeout=30 ubuntu@$IP"
$SCP  = "scp -i `"$KEY`" -o StrictHostKeyChecking=no"
$ROOT = Split-Path $PSScriptRoot -Parent

function Run($cmd) {
    Write-Host "`n$ $cmd" -ForegroundColor Cyan
    Invoke-Expression $cmd
    if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) {
        Write-Host "FAILED with exit code $LASTEXITCODE" -ForegroundColor Red
        exit $LASTEXITCODE
    }
}

Write-Host "`n=== Step 1: Build Java Backend JAR ===" -ForegroundColor Yellow
Set-Location "$ROOT\backend"
Run "mvn package -DskipTests -q"
Set-Location $ROOT

Write-Host "`n=== Step 2: Ensure remote /app/build directory on EC2 ===" -ForegroundColor Yellow
Invoke-Expression "$SSH `"sudo mkdir -p /app/build && sudo chown -R ubuntu:ubuntu /app`""

Write-Host "`n=== Step 3: Copy JAR, Dockerfile, and Compose to EC2 ===" -ForegroundColor Yellow
Run "$SCP `"$ROOT\backend\target\*.jar`" ubuntu@${IP}:/app/build/app.jar"
Run "$SCP `"$ROOT\backend\Dockerfile`" ubuntu@${IP}:/app/build/Dockerfile"
Run "$SCP `"$PSScriptRoot\docker-compose.prod.yml`" ubuntu@${IP}:/app/docker-compose.yml"

Write-Host "`n=== Step 4: Build backend Docker image on EC2 ===" -ForegroundColor Yellow
Run "$SSH `"cd /app/build && docker build --build-arg JAR_FILE=app.jar -t digital-library-backend:latest .`""

Write-Host "`n=== Step 5: Restart containers on EC2 ===" -ForegroundColor Yellow
Run "$SSH `"cd /app && docker compose down --remove-orphans && docker compose up -d`""

Write-Host "`n=== Step 6: Wait 30s for services to start ===" -ForegroundColor Yellow
Start-Sleep 30

Write-Host "`n=== Step 7: Smoke test backend health & login ===" -ForegroundColor Yellow
try {
    $resp = Invoke-RestMethod "http://${IP}:8000/api/health" -TimeoutSec 15
    Write-Host ("Backend Health OK: " + ($resp | ConvertTo-Json -Depth 3)) -ForegroundColor Green
} catch {
    Write-Host "Health check warning: $_" -ForegroundColor DarkYellow
}

try {
    $body = '{"email":"admin@library.com","password":"admin123"}'
    $login = Invoke-RestMethod -Method POST -Uri "http://${IP}:8000/api/auth/login" -Body $body -ContentType "application/json" -TimeoutSec 15
    Write-Host ("Admin Login OK! Token: " + $login.data.token.Substring(0,25) + "...") -ForegroundColor Green
} catch {
    Write-Host "Login failed: $_" -ForegroundColor Red
}

Write-Host @"

==========================================
  REDEPLOYMENT COMPLETE!
  Frontend : http://$IP:3000
  Login    : http://$IP:3000/login
  Backend  : http://$IP:8000/api/health
==========================================
"@ -ForegroundColor Green
