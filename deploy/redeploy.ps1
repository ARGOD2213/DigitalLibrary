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

Write-Host "`n=== Step 1: Build frontend Docker image (no cache) ===" -ForegroundColor Yellow
Set-Location $ROOT
Run "docker build --no-cache -t digital-library-frontend:latest ./frontend"

Write-Host "`n=== Step 2: Save both images to archive ===" -ForegroundColor Yellow
$TAR = "$PSScriptRoot\images.tar"
Run "docker save -o `"$TAR`" digital-library-frontend:latest digital-library-backend:latest"

Write-Host "`n=== Step 3: Ensure /app dir on EC2 ===" -ForegroundColor Yellow
Invoke-Expression "$SSH `"sudo mkdir -p /app && sudo chown ubuntu:ubuntu /app`""

Write-Host "`n=== Step 4: Copy images to EC2 ===" -ForegroundColor Yellow
Run "$SCP `"$TAR`" ubuntu@${IP}:/app/images.tar"
Remove-Item $TAR -Force -ErrorAction SilentlyContinue

Write-Host "`n=== Step 5: Load images on EC2 ===" -ForegroundColor Yellow
Run "$SSH `"docker load -i /app/images.tar && rm -f /app/images.tar`""

Write-Host "`n=== Step 6: Copy fresh compose file ===" -ForegroundColor Yellow
Run "$SCP `"$PSScriptRoot\docker-compose.prod.yml`" ubuntu@${IP}:/app/docker-compose.yml"

Write-Host "`n=== Step 7: Restart containers on EC2 ===" -ForegroundColor Yellow
Run "$SSH `"cd /app && docker compose down --remove-orphans && docker compose up -d`""

Write-Host "`n=== Step 8: Wait 30s for Spring Boot to start ===" -ForegroundColor Yellow
Start-Sleep 30

Write-Host "`n=== Step 9: Smoke test backend /api/books ===" -ForegroundColor Yellow
try {
    $resp = Invoke-RestMethod "http://${IP}:3000/api/books" -TimeoutSec 15
    Write-Host "Backend OK" -ForegroundColor Green
} catch {
    Write-Host "Backend may still be starting: $_" -ForegroundColor DarkYellow
}

Write-Host @"

==========================================
  REDEPLOYMENT COMPLETE!
  Frontend : http://$IP:3000
  Login    : http://$IP:3000/login
  Register : http://$IP:3000/register
  Backend  : http://$IP:8000/api/books
==========================================
"@ -ForegroundColor Green
