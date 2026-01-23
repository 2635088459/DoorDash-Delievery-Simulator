#!/bin/bash

# =======================================================
# Cognito 用户角色批量更新脚本
# 
# 功能：为所有现有 Cognito 用户添加 custom:role 属性
# 使用方法：./update-cognito-roles.sh
# =======================================================

set -e  # 遇到错误立即退出

# 配置
USER_POOL_ID="us-east-1_a6gt5CsAi"
REGION="us-east-1"

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Cognito 用户角色批量更新工具${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 检查 AWS CLI 是否安装
if ! command -v aws &> /dev/null; then
    echo -e "${RED}错误: AWS CLI 未安装！${NC}"
    echo "请访问 https://aws.amazon.com/cli/ 安装 AWS CLI"
    exit 1
fi

echo -e "${YELLOW}正在从数据库获取用户信息...${NC}"

# 从 PostgreSQL 数据库获取所有用户的 email 和 role
USERS=$(docker exec doordash-postgres psql -U postgres -d doordash_db -t -A -F"," -c \
    "SELECT email, role, cognito_sub FROM users WHERE cognito_sub IS NOT NULL;")

if [ -z "$USERS" ]; then
    echo -e "${RED}错误: 数据库中没有找到用户数据${NC}"
    exit 1
fi

echo -e "${GREEN}找到以下用户:${NC}"
echo "$USERS" | while IFS=',' read -r email role cognito_sub; do
    echo "  - $email (角色: $role)"
done
echo ""

# 提示用户确认
read -p "是否继续更新这些用户的 Cognito custom:role 属性? (y/n) " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}操作已取消${NC}"
    exit 0
fi

echo ""
echo -e "${YELLOW}开始更新用户属性...${NC}"
echo ""

# 更新计数器
SUCCESS_COUNT=0
FAIL_COUNT=0

# 遍历每个用户并更新 custom:role
echo "$USERS" | while IFS=',' read -r email role cognito_sub; do
    echo -e "${YELLOW}正在更新: $email${NC}"
    
    # 使用 AWS CLI 更新用户属性
    if aws cognito-idp admin-update-user-attributes \
        --user-pool-id "$USER_POOL_ID" \
        --username "$cognito_sub" \
        --user-attributes Name=custom:role,Value="$role" \
        --region "$REGION" 2>&1; then
        
        echo -e "${GREEN}✅ 成功: $email -> custom:role = $role${NC}"
        ((SUCCESS_COUNT++))
    else
        echo -e "${RED}❌ 失败: $email${NC}"
        ((FAIL_COUNT++))
    fi
    echo ""
done

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}更新完成！${NC}"
echo -e "${GREEN}成功: $SUCCESS_COUNT 个用户${NC}"
if [ $FAIL_COUNT -gt 0 ]; then
    echo -e "${RED}失败: $FAIL_COUNT 个用户${NC}"
fi
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${YELLOW}提示: 现在用户重新登录后，JWT Token 将包含 custom:role 字段${NC}"
