# 📚 Digital Library — Complete Operations Guide
### Beginner-Friendly: Start, Stop, Deploy, Monitor

---

## 🖥️ About Your App

Your **Digital Library** is running 100% on **AWS Cloud**. You do NOT need your PC to be on for the app to stay live.

| Component | Where it runs |
|---|---|
| **Frontend** (React website) | EC2 Server in AWS |
| **Backend** (Spring Boot API) | EC2 Server in AWS |
| **Database** (PostgreSQL) | AWS RDS (managed) |
| **Cache** (Redis) | EC2 Server in AWS |

**Your Server IP:** `13.233.106.4`  
**SSH Key file:** `deploy\digital-library-key.pem`

---

## ✅ SECTION 1 — Can I Close the IDE?

**Yes, 100% safe to close.**

The app runs on AWS, not your laptop. Closing VS Code / the IDE has zero effect on the live application.

The only time you need the IDE open is when you want to:
- Change code
- Redeploy the app
- Check logs from your PC

---

## 🟢 SECTION 2 — How to CHECK if App is Running

### Option A — Open your browser
Go to these URLs:
- **App home:** http://13.233.106.4:3000
- **Login page:** http://13.233.106.4:3000/login
- **Register:** http://13.233.106.4:3000/register
- **API check:** http://13.233.106.4:3000/api/books

### Option B — Check from terminal
Open PowerShell in the `Digital Library` folder and run:

```powershell
ssh -i "deploy\digital-library-key.pem" -o StrictHostKeyChecking=no ubuntu@13.233.106.4 "docker ps"
```

You should see 3 containers running:
```
digital-library-redis     → running (healthy)
digital-library-backend   → running
digital-library-frontend  → running
```

---

## 🛑 SECTION 3 — How to STOP the App

> ⚠️ Stopping the app means the website goes offline. Only do this if needed.

### Stop all containers (app offline, server still running):
```powershell
ssh -i "deploy\digital-library-key.pem" -o StrictHostKeyChecking=no ubuntu@13.233.106.4 "cd /app && docker compose down"
```

### Stop the EC2 server entirely (saves AWS cost):
1. Open [AWS Console → EC2](https://ap-south-1.console.aws.amazon.com/ec2/home?region=ap-south-1#Instances)
2. Find `digital-library-server`
3. Select it → click **Instance State** → **Stop**

> 💡 **Note:** Stopping EC2 will give it a new IP address when restarted. Update the IP in your `deploy\deployment-info.json`.

---

## 🚀 SECTION 4 — How to START the App (after stopping)

### If containers stopped but EC2 is running:
```powershell
ssh -i "deploy\digital-library-key.pem" -o StrictHostKeyChecking=no ubuntu@13.233.106.4 "cd /app && docker compose up -d"
```

### If EC2 was stopped and restarted (new IP):
1. Get the new IP from AWS Console → EC2 → your instance
2. Update `deploy\deployment-info.json` with new IP
3. Run:
```powershell
ssh -i "deploy\digital-library-key.pem" -o StrictHostKeyChecking=no ubuntu@NEW_IP "cd /app && docker compose up -d"
```

---

## 🚢 SECTION 5 — How to DEPLOY Code Changes

Use this whenever you change code and want the live site updated.

> Open PowerShell in the `Digital Library` folder.

### Step 1 — Make your code changes (edit files in `frontend/src` or `backend/src`)

### Step 2 — Run the one-shot redeploy script:
```powershell
powershell -ExecutionPolicy Bypass -File "deploy\redeploy.ps1"
```

This script automatically:
1. Rebuilds the Docker image with your new code
2. Packages both images into a tar file
3. Uploads to EC2
4. Restarts all containers with zero downtime

### Step 3 — Wait ~5 minutes, then check:
http://13.233.106.4:3000

---

## 📋 SECTION 6 — How to VIEW LOGS

### View live backend logs (Spring Boot errors):
```powershell
ssh -i "deploy\digital-library-key.pem" -o StrictHostKeyChecking=no ubuntu@13.233.106.4 "docker logs digital-library-backend --tail 100 -f"
```
> Press `Ctrl+C` to stop watching

### View only errors:
```powershell
ssh -i "deploy\digital-library-key.pem" -o StrictHostKeyChecking=no ubuntu@13.233.106.4 "docker logs digital-library-backend 2>&1 | grep -i 'error\|exception'"
```

### View frontend (nginx) logs:
```powershell
ssh -i "deploy\digital-library-key.pem" -o StrictHostKeyChecking=no ubuntu@13.233.106.4 "docker logs digital-library-frontend --tail 50"
```

### View ALL containers at once:
```powershell
ssh -i "deploy\digital-library-key.pem" -o StrictHostKeyChecking=no ubuntu@13.233.106.4 "docker compose -f /app/docker-compose.yml logs --tail 30"
```

---

## ☁️ SECTION 7 — How to View Logs in AWS CloudWatch

CloudWatch lets you see logs directly in the AWS web console — no terminal needed.

### Step 1 — Set up CloudWatch Agent on EC2

Run this once from your PowerShell:

```powershell
ssh -i "deploy\digital-library-key.pem" -o StrictHostKeyChecking=no ubuntu@13.233.106.4 @"
# Install CloudWatch agent
sudo apt-get install -y amazon-cloudwatch-agent

# Create config
sudo tee /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json > /dev/null << 'EOF'
{
  "logs": {
    "logs_collected": {
      "files": {
        "collect_list": [
          {
            "file_path": "/var/lib/docker/containers/*/*.log",
            "log_group_name": "/digital-library/docker",
            "log_stream_name": "{instance_id}",
            "multi_line_start_pattern": "^[0-9]"
          }
        ]
      }
    }
  }
}
EOF

# Start agent
sudo systemctl start amazon-cloudwatch-agent
sudo systemctl enable amazon-cloudwatch-agent
echo CloudWatch setup done
"@
```

### Step 2 — Give EC2 permission to write to CloudWatch

1. Open [AWS Console → IAM → Roles](https://console.aws.amazon.com/iam/home#/roles)
2. Click **Create role**
3. Choose **EC2** as the trusted entity
4. Attach policy: **CloudWatchAgentServerPolicy**
5. Name it: `digital-library-ec2-role`
6. Click **Create role**

Then attach it to your EC2:
1. Go to [EC2 Console](https://ap-south-1.console.aws.amazon.com/ec2/home?region=ap-south-1#Instances)
2. Select `digital-library-server`
3. **Actions → Security → Modify IAM Role**
4. Select `digital-library-ec2-role` → **Update**

### Step 3 — View logs in CloudWatch Console

1. Open [CloudWatch → Log Groups](https://ap-south-1.console.aws.amazon.com/cloudwatch/home?region=ap-south-1#logsV2:log-groups)
2. Find `/digital-library/docker`
3. Click it → click the stream → see all logs

---

## 🔑 SECTION 8 — Your AWS Resources (Quick Reference)

| Resource | ID / Name |
|---|---|
| EC2 Instance | `i-08d503f7f23987066` |
| EC2 Public IP | `13.233.106.4` |
| EC2 Name | `digital-library-server` |
| RDS Instance | `digital-library-db` |
| RDS Endpoint | `digital-library-db.c7aku04iuzxy.ap-south-1.rds.amazonaws.com` |
| AWS Region | `ap-south-1` (Mumbai) |
| SSH Key | `deploy\digital-library-key.pem` |
| DB Name | `digital_library` |
| DB User | `dlibadmin` |

---

## 🚨 SECTION 9 — Common Problems & Fixes

### ❌ "Failed to fetch" error on website
**Cause:** Backend container crashed  
**Fix:**
```powershell
ssh -i "deploy\digital-library-key.pem" -o StrictHostKeyChecking=no ubuntu@13.233.106.4 "cd /app && docker compose restart backend"
```

### ❌ Registration / Login not working
**Check backend errors:**
```powershell
ssh -i "deploy\digital-library-key.pem" -o StrictHostKeyChecking=no ubuntu@13.233.106.4 "docker logs digital-library-backend --tail 50 2>&1 | grep -i 'error\|exception'"
```

### ❌ Website not loading at all
**Check if containers are running:**
```powershell
ssh -i "deploy\digital-library-key.pem" -o StrictHostKeyChecking=no ubuntu@13.233.106.4 "docker ps"
```
**If empty, restart:**
```powershell
ssh -i "deploy\digital-library-key.pem" -o StrictHostKeyChecking=no ubuntu@13.233.106.4 "cd /app && docker compose up -d"
```

### ❌ EC2 got a new IP after restart
1. Check new IP in [AWS EC2 Console](https://ap-south-1.console.aws.amazon.com/ec2/home?region=ap-south-1#Instances)
2. Update `deploy\deployment-info.json`
3. Update `deploy\redeploy.ps1` (line: `$IP = "NEW_IP"`)

---

## 📞 SECTION 10 — Quick SSH Commands Cheat Sheet

```powershell
# SSH into server
ssh -i "deploy\digital-library-key.pem" ubuntu@13.233.106.4

# See all running containers
docker ps

# See backend logs live
docker logs digital-library-backend -f

# Restart everything
cd /app && docker compose restart

# Stop everything
cd /app && docker compose down

# Start everything
cd /app && docker compose up -d

# See disk usage
df -h

# See memory usage
free -h
```

---

*Generated: 2026-08-11 | Digital Library v1.0 | AWS ap-south-1 (Mumbai)*
