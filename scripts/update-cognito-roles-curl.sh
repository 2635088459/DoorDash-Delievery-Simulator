#!/bin/bash

# =======================================================
# Cognito 用户角色批量更新脚本 (使用 AWS API + curl)
# 
# 功能：为所有现有 Cognito 用户添加 custom:role 属性
# 使用方法：./update-cognito-roles-curl.sh
# =======================================================

set -e  # 遇到错误立即退出

# 配置
USER_POOL_ID="us-east-1_a6gt5CsAi"
REGION="us-east-1"
SERVICE="cognito-idp"
HOST="${SERVICE}.${REGION}.amazonaws.com"

# 读取 AWS 凭证
if [ ! -f .env ]; then
    echo "错误: .env 文件不存在"
    exit 1
fi

source .env

if [ -z "$AWS_ACCESS_KEY_ID" ] || [ -z "$AWS_SECRET_ACCESS_KEY" ]; then
    echo "错误: AWS 凭证未在 .env 文件中配置"
    exit 1
fi

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Cognito 用户角色批量更新工具 (curl 版本)${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# AWS Signature V4 签名函数
function aws_sign() {
    local method="$1"
    local uri="$2"
    local query_string="$3"
    local payload="$4"
    local content_type="$5"
    
    # 时间戳
    local timestamp=$(date -u +"%Y%m%dT%H%M%SZ")
    local datestamp=$(date -u +"%Y%m%d")
    
    # 创建 canonical request
    local canonical_uri="${uri}"
    local canonical_querystring="${query_string}"
    local canonical_headers="content-type:${content_type}
host:${HOST}
x-amz-date:${timestamp}
"
    local signed_headers="content-type;host;x-amz-date"
    local payload_hash=$(echo -n "${payload}" | openssl dgst -sha256 -hex | awk '{print $2}')
    
    local canonical_request="${method}
${canonical_uri}
${canonical_querystring}
${canonical_headers}
${signed_headers}
${payload_hash}"
    
    # 创建 string to sign
    local algorithm="AWS4-HMAC-SHA256"
    local credential_scope="${datestamp}/${REGION}/${SERVICE}/aws4_request"
    local canonical_request_hash=$(echo -n "${canonical_request}" | openssl dgst -sha256 -hex | awk '{print $2}')
    
    local string_to_sign="${algorithm}
${timestamp}
${credential_scope}
${canonical_request_hash}"
    
    # 计算签名
    local kSecret="AWS4${AWS_SECRET_ACCESS_KEY}"
    local kDate=$(echo -n "${datestamp}" | openssl dgst -sha256 -hmac "${kSecret}" -binary)
    local kRegion=$(echo -n "${REGION}" | openssl dgst -sha256 -hmac "${kDate}" -binary)
    local kService=$(echo -n "${SERVICE}" | openssl dgst -sha256 -hmac "${kRegion}" -binary)
    local kSigning=$(echo -n "aws4_request" | openssl dgst -sha256 -hmac "${kService}" -binary)
    local signature=$(echo -n "${string_to_sign}" | openssl dgst -sha256 -hmac "${kSigning}" -hex | awk '{print $2}')
    
    # 创建 authorization header
    local authorization_header="${algorithm} Credential=${AWS_ACCESS_KEY_ID}/${credential_scope}, SignedHeaders=${signed_headers}, Signature=${signature}"
    
    # 发送请求
    curl -s -X POST "https://${HOST}${uri}" \
        -H "Content-Type: ${content_type}" \
        -H "X-Amz-Date: ${timestamp}" \
        -H "X-Amz-Target: AWSCognitoIdentityProviderService.AdminUpdateUserAttributes" \
        -H "Authorization: ${authorization_header}" \
        -d "${payload}"
}

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
    
    # 构造 JSON payload
    PAYLOAD=$(cat <<EOF
{
    "UserPoolId": "$USER_POOL_ID",
    "Username": "$cognito_sub",
    "UserAttributes": [
        {
            "Name": "custom:role",
            "Value": "$role"
        }
    ]
}
EOF
)
    
    # 调用 AWS API
    RESPONSE=$(curl -s -X POST \
        "https://${HOST}/" \
        -H "Content-Type: application/x-amz-json-1.1" \
        -H "X-Amz-Target: AWSCognitoIdentityProviderService.AdminUpdateUserAttributes" \
        --aws-sigv4 "aws:amz:${REGION}:${SERVICE}" \
        --user "${AWS_ACCESS_KEY_ID}:${AWS_SECRET_ACCESS_KEY}" \
        -d "$PAYLOAD" 2>&1)
    
    # 检查响应
    if echo "$RESPONSE" | grep -q "error" || echo "$RESPONSE" | grep -q "Error"; then
        echo -e "${RED}❌ 失败: $email${NC}"
        echo -e "${RED}   响应: $RESPONSE${NC}"
        ((FAIL_COUNT++))
    else
        echo -e "${GREEN}✅ 成功: $email -> custom:role = $role${NC}"
        ((SUCCESS_COUNT++))
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
