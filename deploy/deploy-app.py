"""
Digital Library — EC2 App Deployment Script
Reads deployment-info.json, creates .env on EC2, pushes docker-compose + Dockerfiles via SCP
then SSHs to EC2 to run docker compose up
"""
import json
import os
import subprocess
import sys
import time

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_DIR = os.path.dirname(SCRIPT_DIR)
INFO_FILE = os.path.join(SCRIPT_DIR, "deployment-info.json")

with open(INFO_FILE) as f:
    info = json.load(f)

IP = info["EC2_PUBLIC_IP"]
KEY = info["SSH_KEY"]
RDS_HOST = info["RDS_ENDPOINT"]
RDS_DB = info["RDS_DB"]
RDS_USER = info["RDS_USER"]
RDS_PASS = info["RDS_PASS"]

SSH = f'ssh -i "{KEY}" -o StrictHostKeyChecking=no -o ConnectTimeout=30 ubuntu@{IP}'
SCP = f'scp -i "{KEY}" -o StrictHostKeyChecking=no -r'

print(f"Deploying to EC2: {IP}")
print(f"Using RDS: {RDS_HOST}")

# ─── Create production .env for EC2 ───────────────────────────────
env_content = f"""POSTGRES_DB={RDS_DB}
POSTGRES_USER={RDS_USER}
POSTGRES_PASSWORD={RDS_PASS}

APP_JWT_SECRET=prod-jwt-secret-digital-library-2026-super-secure-key-xyz
APP_JWT_EXPIRATION_MS=86400000
APP_JWT_REFRESH_EXPIRATION_DAYS=7
APP_STORAGE_LOCAL_FOLDER=local-storage

AWS_REGION=ap-south-1
AWS_ACCESS_KEY_ID={os.environ.get('AWS_ACCESS_KEY_ID', '')}
AWS_SECRET_ACCESS_KEY={os.environ.get('AWS_SECRET_ACCESS_KEY', '')}
AWS_S3_BUCKET_NAME=your-digital-library-books-storage
AWS_SES_FROM_EMAIL=chintalamahindra163@gmail.com
AWS_MOCK_ENABLED=false

REDIS_HOST=redis
REDIS_PORT=6379
SPRING_CACHE_TYPE=redis
"""

env_path = os.path.join(SCRIPT_DIR, "prod.env")
with open(env_path, "w") as f:
    f.write(env_content)
print("Created prod.env")

# ─── Create production docker-compose that uses RDS instead of local postgres ───
compose_content = f"""services:

  redis:
    image: redis:7-alpine
    container_name: digital-library-redis
    restart: unless-stopped
    command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5

  backend:
    image: digital-library-backend:latest
    container_name: digital-library-backend
    restart: unless-stopped
    ports:
      - "8000:8000"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://{RDS_HOST}:5432/{RDS_DB}
      SPRING_DATASOURCE_USERNAME: {RDS_USER}
      SPRING_DATASOURCE_PASSWORD: {RDS_PASS}
      APP_JWT_SECRET: prod-jwt-secret-digital-library-2026-super-secure-key-xyz
      APP_JWT_EXPIRATION_MS: 86400000
      APP_JWT_REFRESH_EXPIRATION_DAYS: 7
      REDIS_HOST: redis
      REDIS_PORT: 6379
      SPRING_CACHE_TYPE: redis
      AWS_REGION: ap-south-1
      AWS_ACCESS_KEY_ID: {os.environ.get('AWS_ACCESS_KEY_ID', '')}
      AWS_SECRET_ACCESS_KEY: {os.environ.get('AWS_SECRET_ACCESS_KEY', '')}
      AWS_S3_BUCKET_NAME: your-digital-library-books-storage
      AWS_SES_FROM_EMAIL: chintalamahindra163@gmail.com
      AWS_MOCK_ENABLED: "false"
      JAVA_TOOL_OPTIONS: "-Duser.timezone=Asia/Kolkata"
    depends_on:
      redis:
        condition: service_healthy

  frontend:
    image: digital-library-frontend:latest
    container_name: digital-library-frontend
    restart: unless-stopped
    ports:
      - "3000:80"
    depends_on:
      - backend
"""

compose_path = os.path.join(SCRIPT_DIR, "docker-compose.prod.yml")
with open(compose_path, "w") as f:
    f.write(compose_content)
print("Created docker-compose.prod.yml (uses RDS, no local postgres)")

def run(cmd, check=True, capture=False):
    print(f"$ {cmd}")
    r = subprocess.run(cmd, shell=True, capture_output=capture, text=True)
    if capture:
        return r.stdout.strip()
    if check and r.returncode != 0:
        print(f"Command failed with code {r.returncode}")
        sys.exit(1)
    return r.returncode

print("\n=== Waiting for EC2 SSH to be ready ===")
for i in range(12):
    result = subprocess.run(f'{SSH} "echo ready"', shell=True, capture_output=True, text=True, timeout=30)
    if result.returncode == 0:
        print("SSH ready!")
        break
    print(f"  Waiting... ({i+1}/12)")
    time.sleep(10)

print("\n=== Creating /app directory on EC2 ===")
run(f'{SSH} "sudo mkdir -p /app && sudo chown ubuntu:ubuntu /app"')

print("\n=== Saving Docker images to local archive ===")
tar_path = os.path.join(SCRIPT_DIR, "images.tar")
run(f'docker save -o "{tar_path}" digital-library-backend:latest digital-library-frontend:latest')

print("\n=== Transferring images archive to EC2 via SCP ===")
run(f'{SCP} "{tar_path}" ubuntu@{IP}:/app/images.tar')

print("\n=== Loading Docker images on EC2 ===")
run(f'{SSH} "docker load -i /app/images.tar && rm -f /app/images.tar"')

if os.path.exists(tar_path):
    try:
        os.remove(tar_path)
    except Exception:
        pass

print("\n=== Copying compose file ===")
run(f'{SCP} "{compose_path}" ubuntu@{IP}:/app/docker-compose.yml')

print("\n=== Starting containers on EC2 ===")
run(f'{SSH} "cd /app && docker compose up -d"')

print(f"""
==========================================
  DEPLOYMENT COMPLETE!
  Frontend : http://{IP}:3000
  Backend  : http://{IP}:8000
  SSH      : ssh -i {os.path.basename(KEY)} ubuntu@{IP}
==========================================
""")
