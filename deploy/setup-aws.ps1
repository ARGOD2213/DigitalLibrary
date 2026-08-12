#!/usr/bin/env pwsh
# ===================================================================
# Digital Library — AWS Full Setup Script
# Creates: RDS PostgreSQL + EC2 + Security Groups
# ===================================================================

$ErrorActionPreference = "Stop"

# ─── CONFIG ─────────────────────────────────────────────────────────
$REGION       = "ap-south-1"
$AWS_KEY      = $env:AWS_ACCESS_KEY_ID
$AWS_SECRET   = $env:AWS_SECRET_ACCESS_KEY

$DB_NAME      = "digital_library"
$DB_USER      = "dlibadmin"
$DB_PASS      = "DigLib@2026#Secure"

$EC2_KEY_NAME = "digital-library-key"
$SG_NAME      = "digital-library-sg"
$RDS_ID       = "digital-library-db"
$EC2_TAG      = "digital-library-server"
$AMI_ID       = "ami-0522ab6e1ddcc7055"  # Ubuntu 22.04 LTS ap-south-1

# Configure AWS credentials
$env:AWS_ACCESS_KEY_ID     = $AWS_KEY
$env:AWS_SECRET_ACCESS_KEY = $AWS_SECRET
$env:AWS_DEFAULT_REGION    = $REGION

Write-Host "`n=== Step 1: Get Default VPC & Subnets ===" -ForegroundColor Cyan
$VPC_ID = (aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --query "Vpcs[0].VpcId" --output text)
Write-Host "VPC: $VPC_ID"

$SUBNET_IDS = @(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" --query "Subnets[*].SubnetId" --output text)
$SUBNET_ARR = ($SUBNET_IDS -join " ") -split "\s+"
Write-Host "Subnets: $($SUBNET_ARR -join ', ')"

Write-Host "`n=== Step 2: Create Security Group ===" -ForegroundColor Cyan
$EXISTING_SG = (aws ec2 describe-security-groups --filters "Name=group-name,Values=$SG_NAME" "Name=vpc-id,Values=$VPC_ID" --query "SecurityGroups[0].GroupId" --output text 2>$null)

if ($EXISTING_SG -and $EXISTING_SG -ne "None") {
    $SG_ID = $EXISTING_SG
    Write-Host "Security group already exists: $SG_ID"
} else {
    $SG_ID = (aws ec2 create-security-group --group-name $SG_NAME --description "Digital Library App Security Group" --vpc-id $VPC_ID --query "GroupId" --output text)
    Write-Host "Created security group: $SG_ID"
    foreach ($PORT in @(22, 80, 443, 3000, 8000, 5432)) {
        aws ec2 authorize-security-group-ingress --group-id $SG_ID --protocol tcp --port $PORT --cidr "0.0.0.0/0" 2>$null | Out-Null
    }
    Write-Host "Security group rules added (ports 22,80,443,3000,8000,5432)."
}

Write-Host "`n=== Step 3: Create RDS Subnet Group ===" -ForegroundColor Cyan
$RDS_SUBNET_GROUP = "digital-library-db-subnet-group"
aws rds create-db-subnet-group --db-subnet-group-name $RDS_SUBNET_GROUP --db-subnet-group-description "Digital Library DB Subnet Group" --subnet-ids $SUBNET_ARR[0] $SUBNET_ARR[1] 2>$null | Out-Null
Write-Host "Subnet group ready: $RDS_SUBNET_GROUP"

Write-Host "`n=== Step 4: Create RDS PostgreSQL Instance ===" -ForegroundColor Cyan
$EXISTING_RDS = (aws rds describe-db-instances --db-instance-identifier $RDS_ID --query "DBInstances[0].DBInstanceStatus" --output text 2>$null)
if ($EXISTING_RDS -and $EXISTING_RDS -ne "") {
    Write-Host "RDS instance '$RDS_ID' already exists. Status: $EXISTING_RDS"
} else {
    aws rds create-db-instance `
        --db-instance-identifier $RDS_ID `
        --db-instance-class db.t3.micro `
        --engine postgres `
        --engine-version "16.4" `
        --master-username $DB_USER `
        --master-user-password $DB_PASS `
        --db-name $DB_NAME `
        --allocated-storage 20 `
        --storage-type gp2 `
        --vpc-security-group-ids $SG_ID `
        --db-subnet-group-name $RDS_SUBNET_GROUP `
        --publicly-accessible `
        --backup-retention-period 1 `
        --no-multi-az | Out-Null
    Write-Host "RDS instance creation started (~5 min to be available)..."
}

Write-Host "`n=== Step 5: Create EC2 Key Pair ===" -ForegroundColor Cyan
$KEY_EXISTS = (aws ec2 describe-key-pairs --key-names $EC2_KEY_NAME --query "KeyPairs[0].KeyName" --output text 2>$null)
$KEY_PATH = "$PSScriptRoot\$EC2_KEY_NAME.pem"
if ($KEY_EXISTS -ne $EC2_KEY_NAME) {
    $KEY_MATERIAL = (aws ec2 create-key-pair --key-name $EC2_KEY_NAME --query "KeyMaterial" --output text)
    $KEY_MATERIAL | Out-File -FilePath $KEY_PATH -Encoding ascii -NoNewline
    Write-Host "Key pair created: $KEY_PATH"
} else {
    Write-Host "Key pair '$EC2_KEY_NAME' already exists."
}

Write-Host "`n=== Step 6: Launch EC2 Instance (t3.small, Ubuntu 22.04) ===" -ForegroundColor Cyan
$EXISTING_EC2 = (aws ec2 describe-instances --filters "Name=tag:Name,Values=$EC2_TAG" "Name=instance-state-name,Values=running,pending,stopped" --query "Reservations[0].Instances[0].InstanceId" --output text 2>$null)

if ($EXISTING_EC2 -and $EXISTING_EC2 -ne "None") {
    $INSTANCE_ID = $EXISTING_EC2
    Write-Host "EC2 instance already exists: $INSTANCE_ID"
} else {
    $USER_DATA = @"
#!/bin/bash
apt-get update -y
apt-get install -y docker.io docker-compose-plugin git curl postgresql-client
systemctl start docker
systemctl enable docker
usermod -aG docker ubuntu
"@
    $USER_DATA_B64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($USER_DATA))
    $INSTANCE_ID = (aws ec2 run-instances `
        --image-id $AMI_ID `
        --instance-type t3.small `
        --key-name $EC2_KEY_NAME `
        --security-group-ids $SG_ID `
        --subnet-id $SUBNET_ARR[0] `
        --associate-public-ip-address `
        --user-data $USER_DATA_B64 `
        --count 1 `
        --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$EC2_TAG}]" `
        --query "Instances[0].InstanceId" `
        --output text)
    Write-Host "EC2 instance launched: $INSTANCE_ID"
}

Write-Host "`n=== Waiting for EC2 to be running ===" -ForegroundColor Yellow
aws ec2 wait instance-running --instance-ids $INSTANCE_ID
$PUBLIC_IP = (aws ec2 describe-instances --instance-ids $INSTANCE_ID --query "Reservations[0].Instances[0].PublicIpAddress" --output text)
Write-Host "EC2 Public IP: $PUBLIC_IP"

Write-Host "`n=== Waiting for RDS to be available (this takes ~5 min) ===" -ForegroundColor Yellow
aws rds wait db-instance-available --db-instance-identifier $RDS_ID
$RDS_ENDPOINT = (aws rds describe-db-instances --db-instance-identifier $RDS_ID --query "DBInstances[0].Endpoint.Address" --output text)
Write-Host "RDS Endpoint: $RDS_ENDPOINT"

# Save deployment info
$INFO = @"
EC2_PUBLIC_IP=$PUBLIC_IP
EC2_INSTANCE_ID=$INSTANCE_ID
RDS_ENDPOINT=$RDS_ENDPOINT
RDS_PORT=5432
RDS_DB=$DB_NAME
RDS_USER=$DB_USER
RDS_PASS=$DB_PASS
SSH_KEY=$KEY_PATH
REGION=$REGION
"@
$INFO | Out-File -FilePath "$PSScriptRoot\deployment-info.txt" -Encoding ascii

Write-Host "`n==========================================" -ForegroundColor Green
Write-Host "  Infrastructure READY!" -ForegroundColor Green
Write-Host "  EC2 IP    : $PUBLIC_IP" -ForegroundColor Green
Write-Host "  RDS Host  : $RDS_ENDPOINT" -ForegroundColor Green
Write-Host "  Frontend  : http://$PUBLIC_IP:3000" -ForegroundColor Green
Write-Host "  Backend   : http://$PUBLIC_IP:8000" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host "Next: run deploy-app.ps1 to push the app to EC2" -ForegroundColor Yellow
