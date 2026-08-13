"""
Digital Library — AWS Provisioning Script
Creates: VPC Security Group, RDS PostgreSQL, EC2 Instance
Then prints SSH + connection info for app deployment
"""
import boto3
import time
import os
import sys
import json
import urllib.request

REGION = "ap-south-1"
AWS_KEY = os.environ.get("AWS_ACCESS_KEY_ID", "")
AWS_SECRET = os.environ.get("AWS_SECRET_ACCESS_KEY", "")

DB_NAME = "digital_library"
DB_USER = "dlibadmin"
DB_PASS = os.environ.get("DB_MASTER_PASSWORD", "")
if not DB_PASS:
    sys.exit("DB_MASTER_PASSWORD env var is required (RDS master password) — refusing to use a hardcoded default.")

EC2_KEY_NAME = "digital-library-key"
SG_NAME = "digital-library-sg"
RDS_ID = "digital-library-db"
EC2_TAG = "digital-library-server"

# Ubuntu 22.04 LTS - ap-south-1
AMI_ID = "ami-0522ab6e1ddcc7055"

session = boto3.Session(
    aws_access_key_id=AWS_KEY,
    aws_secret_access_key=AWS_SECRET,
    region_name=REGION
)

ec2 = session.client("ec2")
rds = session.client("rds")
ec2_res = session.resource("ec2")

def step(msg):
    print(f"\n=== {msg} ===")

# ─── Step 1: Get default VPC ──────────────────────────────────────
step("Step 1: Get Default VPC & Subnets")
vpcs = ec2.describe_vpcs(Filters=[{"Name": "isDefault", "Values": ["true"]}])
vpc_id = vpcs["Vpcs"][0]["VpcId"]
print(f"VPC: {vpc_id}")

subnets = ec2.describe_subnets(Filters=[{"Name": "vpc-id", "Values": [vpc_id]}])
subnet_ids = [s["SubnetId"] for s in subnets["Subnets"]]
print(f"Subnets: {subnet_ids}")

# ─── Step 2: Security Groups ───────────────────────────────────────
step("Step 2: Create/Get Security Groups")

def get_or_create_sg(name, description):
    existing = ec2.describe_security_groups(
        Filters=[{"Name": "group-name", "Values": [name]}, {"Name": "vpc-id", "Values": [vpc_id]}]
    )
    if existing["SecurityGroups"]:
        return existing["SecurityGroups"][0]["GroupId"], False
    sg = ec2.create_security_group(GroupName=name, Description=description, VpcId=vpc_id)
    return sg["GroupId"], True

# App/EC2 security group — SSH restricted to the operator's current IP, web/app ports public.
sg_id, sg_created = get_or_create_sg(SG_NAME, "Digital Library App Security Group (EC2)")
if sg_created:
    print(f"Created app security group: {sg_id}")
    my_ip = urllib.request.urlopen("https://checkip.amazonaws.com", timeout=5).read().decode().strip()
    ssh_cidr = f"{my_ip}/32"
    for port, cidr in [(22, ssh_cidr), (80, "0.0.0.0/0"), (443, "0.0.0.0/0"),
                        (3000, "0.0.0.0/0"), (8000, "0.0.0.0/0")]:
        ec2.authorize_security_group_ingress(
            GroupId=sg_id,
            IpPermissions=[{"IpProtocol": "tcp", "FromPort": port, "ToPort": port, "IpRanges": [{"CidrIp": cidr}]}]
        )
    print(f"Opened ports: 22 (restricted to {ssh_cidr}), 80, 443, 3000, 8000 (public)")
else:
    print(f"App security group already exists: {sg_id}")

# DB security group — Postgres reachable only from the app security group, never the internet.
db_sg_id, db_sg_created = get_or_create_sg(f"{SG_NAME}-db", "Digital Library DB Security Group (RDS)")
if db_sg_created:
    print(f"Created DB security group: {db_sg_id}")
    ec2.authorize_security_group_ingress(
        GroupId=db_sg_id,
        IpPermissions=[{
            "IpProtocol": "tcp", "FromPort": 5432, "ToPort": 5432,
            "UserIdGroupPairs": [{"GroupId": sg_id}]
        }]
    )
    print("Opened port 5432 to the app security group only")
else:
    print(f"DB security group already exists: {db_sg_id}")

# ─── Step 3: RDS Subnet Group ─────────────────────────────────────
step("Step 3: RDS Subnet Group")
try:
    rds.create_db_subnet_group(
        DBSubnetGroupName="digital-library-db-subnet-group",
        DBSubnetGroupDescription="Digital Library DB Subnet Group",
        SubnetIds=subnet_ids[:2]
    )
    print("Created RDS subnet group")
except rds.exceptions.DBSubnetGroupAlreadyExistsFault:
    print("RDS subnet group already exists")

# ─── Step 4: RDS PostgreSQL ───────────────────────────────────────
step("Step 4: Create RDS PostgreSQL Instance")
try:
    existing_rds = rds.describe_db_instances(DBInstanceIdentifier=RDS_ID)
    rds_status = existing_rds["DBInstances"][0]["DBInstanceStatus"]
    print(f"RDS already exists. Status: {rds_status}")
    rds_endpoint = existing_rds["DBInstances"][0].get("Endpoint", {}).get("Address", "")
except Exception:
    rds.create_db_instance(
        DBInstanceIdentifier=RDS_ID,
        DBInstanceClass="db.t3.micro",
        Engine="postgres",
        MasterUsername=DB_USER,
        MasterUserPassword=DB_PASS,
        DBName=DB_NAME,
        AllocatedStorage=20,
        StorageType="gp2",
        VpcSecurityGroupIds=[db_sg_id],
        DBSubnetGroupName="digital-library-db-subnet-group",
        PubliclyAccessible=False,
        BackupRetentionPeriod=1,
        MultiAZ=False,
        Tags=[{"Key": "Name", "Value": "digital-library-db"}]
    )
    print("RDS creation started...")
    rds_endpoint = ""

# ─── Step 5: EC2 Key Pair ─────────────────────────────────────────
step("Step 5: EC2 Key Pair")
key_path = os.path.join(os.path.dirname(__file__), f"{EC2_KEY_NAME}.pem")
try:
    ec2.describe_key_pairs(KeyNames=[EC2_KEY_NAME])
    print(f"Key pair '{EC2_KEY_NAME}' already exists.")
except ec2.exceptions.from_code("InvalidKeyPair.NotFound"):
    kp = ec2.create_key_pair(KeyName=EC2_KEY_NAME)
    with open(key_path, "w") as f:
        f.write(kp["KeyMaterial"])
    print(f"Key pair created: {key_path}")
except Exception:
    kp = ec2.create_key_pair(KeyName=EC2_KEY_NAME)
    with open(key_path, "w") as f:
        f.write(kp["KeyMaterial"])
    print(f"Key pair created: {key_path}")

# ─── Step 6: EC2 Instance ─────────────────────────────────────────
step("Step 6: Launch EC2 Instance (t3.small, Ubuntu 22.04)")
existing_ec2 = ec2.describe_instances(
    Filters=[
        {"Name": "tag:Name", "Values": [EC2_TAG]},
        {"Name": "instance-state-name", "Values": ["running", "pending", "stopped"]}
    ]
)

if existing_ec2["Reservations"]:
    instance_id = existing_ec2["Reservations"][0]["Instances"][0]["InstanceId"]
    print(f"EC2 already exists: {instance_id}")
else:
    user_data = """#!/bin/bash
apt-get update -y
apt-get install -y docker.io docker-compose-plugin git curl postgresql-client
systemctl start docker
systemctl enable docker
usermod -aG docker ubuntu
echo "Setup complete" > /home/ubuntu/setup.log
"""
    reservation = ec2.run_instances(
        ImageId=AMI_ID,
        InstanceType="t3.small",
        KeyName=EC2_KEY_NAME,
        SecurityGroupIds=[sg_id],
        SubnetId=subnet_ids[0],
        MinCount=1,
        MaxCount=1,
        UserData=user_data,
        TagSpecifications=[{
            "ResourceType": "instance",
            "Tags": [{"Key": "Name", "Value": EC2_TAG}]
        }]
    )
    instance_id = reservation["Instances"][0]["InstanceId"]
    print(f"EC2 launched: {instance_id}")

# ─── Wait for EC2 ─────────────────────────────────────────────────
step("Waiting for EC2 to be running...")
waiter = ec2.get_waiter("instance_running")
waiter.wait(InstanceIds=[instance_id])
inst = ec2.describe_instances(InstanceIds=[instance_id])
public_ip = inst["Reservations"][0]["Instances"][0].get("PublicIpAddress", "")
print(f"EC2 Public IP: {public_ip}")

# ─── Wait for RDS ─────────────────────────────────────────────────
step("Waiting for RDS to be available (~5 min)...")
waiter = rds.get_waiter("db_instance_available")
waiter.wait(DBInstanceIdentifier=RDS_ID, WaiterConfig={"Delay": 30, "MaxAttempts": 40})
rds_info = rds.describe_db_instances(DBInstanceIdentifier=RDS_ID)
rds_endpoint = rds_info["DBInstances"][0]["Endpoint"]["Address"]
print(f"RDS Endpoint: {rds_endpoint}")

# ─── Save Deployment Info ──────────────────────────────────────────
info = {
    "EC2_PUBLIC_IP": public_ip,
    "EC2_INSTANCE_ID": instance_id,
    "RDS_ENDPOINT": rds_endpoint,
    "RDS_PORT": 5432,
    "RDS_DB": DB_NAME,
    "RDS_USER": DB_USER,
    "RDS_PASS": DB_PASS,
    "SSH_KEY": key_path,
    "REGION": REGION
}

info_path = os.path.join(os.path.dirname(__file__), "deployment-info.json")
with open(info_path, "w") as f:
    json.dump(info, f, indent=2)

print(f"""
==========================================
  ✅ Infrastructure READY!
  EC2 IP   : {public_ip}
  RDS Host : {rds_endpoint}
  Frontend : http://{public_ip}:3000
  Backend  : http://{public_ip}:8000
  SSH      : ssh -i {EC2_KEY_NAME}.pem ubuntu@{public_ip}
==========================================
Saved info to: {info_path}
Next: run deploy-app.py to push the app to EC2
""")
