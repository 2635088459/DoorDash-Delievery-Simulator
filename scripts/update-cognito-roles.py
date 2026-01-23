#!/usr/bin/env python3
"""
Cognito 用户角色批量更新脚本 (Python 版本)

功能：为所有现有 Cognito 用户添加 custom:role 属性
使用方法：python3 scripts/update-cognito-roles.py
"""

import sys
import os
import subprocess
import json

# 配置
USER_POOL_ID = "us-east-1_a6gt5CsAi"
REGION = "us-east-1"

# 颜色输出
GREEN = '\033[0;32m'
YELLOW = '\033[1;33m'
RED = '\033[0;31m'
NC = '\033[0m'  # No Color

def print_color(text, color):
    print(f"{color}{text}{NC}")

def main():
    print_color("=" * 50, GREEN)
    print_color("Cognito 用户角色批量更新工具 (Python 版本)", GREEN)
    print_color("=" * 50, GREEN)
    print()

    # 检查 boto3 是否安装
    try:
        import boto3
        from botocore.exceptions import ClientError
    except ImportError:
        print_color("错误: boto3 未安装！", RED)
        print("正在尝试安装 boto3...")
        try:
            subprocess.check_call([sys.executable, "-m", "pip", "install", "boto3"])
            import boto3
            from botocore.exceptions import ClientError
            print_color("✅ boto3 安装成功！", GREEN)
        except Exception as e:
            print_color(f"安装失败: {e}", RED)
            print("请手动安装: pip3 install boto3")
            sys.exit(1)

    print_color("正在从数据库获取用户信息...", YELLOW)

    # 从 PostgreSQL 数据库获取所有用户的 email 和 role
    try:
        result = subprocess.run(
            [
                "docker", "exec", "doordash-postgres",
                "psql", "-U", "postgres", "-d", "doordash_db",
                "-t", "-A", "-F,",
                "-c", "SELECT email, role, cognito_sub FROM users WHERE cognito_sub IS NOT NULL;"
            ],
            capture_output=True,
            text=True,
            check=True
        )
        
        users_data = result.stdout.strip()
        if not users_data:
            print_color("错误: 数据库中没有找到用户数据", RED)
            sys.exit(1)
        
        # 解析用户数据
        users = []
        for line in users_data.split('\n'):
            if line.strip():
                email, role, cognito_sub = line.split(',')
                users.append({
                    'email': email,
                    'role': role,
                    'cognito_sub': cognito_sub
                })
        
        print_color(f"找到 {len(users)} 个用户:", GREEN)
        for user in users:
            print(f"  - {user['email']} (角色: {user['role']}, Sub: {user['cognito_sub'][:8]}...)")
        print()
        
    except subprocess.CalledProcessError as e:
        print_color(f"数据库查询失败: {e}", RED)
        sys.exit(1)

    # 提示用户确认
    response = input("是否继续更新这些用户的 Cognito custom:role 属性? (y/n) ")
    if response.lower() != 'y':
        print_color("操作已取消", YELLOW)
        sys.exit(0)

    print()
    print_color("开始更新用户属性...", YELLOW)
    print()

    # 读取 AWS 凭证
    try:
        with open('.env', 'r') as f:
            env_vars = {}
            for line in f:
                if '=' in line and not line.startswith('#'):
                    key, value = line.strip().split('=', 1)
                    env_vars[key] = value
        
        aws_access_key = env_vars.get('AWS_ACCESS_KEY_ID')
        aws_secret_key = env_vars.get('AWS_SECRET_ACCESS_KEY')
        
        if not aws_access_key or not aws_secret_key:
            print_color("错误: 在 .env 文件中未找到 AWS 凭证", RED)
            sys.exit(1)
    except FileNotFoundError:
        print_color("错误: .env 文件不存在", RED)
        sys.exit(1)

    # 初始化 Cognito 客户端
    client = boto3.client(
        'cognito-idp',
        region_name=REGION,
        aws_access_key_id=aws_access_key,
        aws_secret_access_key=aws_secret_key
    )

    # 更新计数器
    success_count = 0
    fail_count = 0

    # 遍历每个用户并更新 custom:role
    for user in users:
        email = user['email']
        role = user['role']
        cognito_sub = user['cognito_sub']
        
        print_color(f"正在更新: {email}", YELLOW)
        
        try:
            # 使用 admin_update_user_attributes 更新用户属性
            response = client.admin_update_user_attributes(
                UserPoolId=USER_POOL_ID,
                Username=cognito_sub,
                UserAttributes=[
                    {
                        'Name': 'custom:role',
                        'Value': role
                    }
                ]
            )
            
            print_color(f"✅ 成功: {email} -> custom:role = {role}", GREEN)
            success_count += 1
            
        except ClientError as e:
            error_code = e.response['Error']['Code']
            error_msg = e.response['Error']['Message']
            print_color(f"❌ 失败: {email}", RED)
            print_color(f"   错误: {error_code} - {error_msg}", RED)
            fail_count += 1
        except Exception as e:
            print_color(f"❌ 失败: {email}", RED)
            print_color(f"   错误: {str(e)}", RED)
            fail_count += 1
        
        print()

    print()
    print_color("=" * 50, GREEN)
    print_color("更新完成！", GREEN)
    print_color(f"成功: {success_count} 个用户", GREEN)
    if fail_count > 0:
        print_color(f"失败: {fail_count} 个用户", RED)
    print_color("=" * 50, GREEN)
    print()
    print_color("提示: 现在用户重新登录后，JWT Token 将包含 custom:role 字段", YELLOW)

if __name__ == "__main__":
    main()
