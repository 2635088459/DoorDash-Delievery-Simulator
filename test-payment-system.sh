#!/bin/bash

# 测试支付系统
# 包括创建支付、处理支付、退款等功能

BASE_URL="http://localhost:8080/api"
TOKEN=""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 打印测试标题
print_test() {
    echo -e "\n${YELLOW}========== $1 ==========${NC}\n"
}

# 打印成功信息
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# 打印错误信息
print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# 1. 登录获取token
print_test "测试1: 用户登录"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "carttest@example.com",
    "password": "Password123!"
  }')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.accessToken')

if [ "$TOKEN" != "null" ] && [ ! -z "$TOKEN" ]; then
    print_success "登录成功，获取到token"
    echo "Token前20字符: ${TOKEN:0:20}..."
else
    print_error "登录失败"
    echo $LOGIN_RESPONSE | jq .
    exit 1
fi

# 2. 创建订单（支付前需要先有订单）
print_test "测试2: 创建订单"
ORDER_RESPONSE=$(curl -s -X POST "$BASE_URL/orders" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 1,
    "items": [
      {
        "menuItemId": 1,
        "quantity": 2,
        "specialInstructions": "不要辣"
      }
    ],
    "deliveryAddress": "123 Main St, City, State 12345",
    "deliveryInstructions": "请放在门口"
  }')

ORDER_ID=$(echo $ORDER_RESPONSE | jq -r '.id')

if [ "$ORDER_ID" != "null" ] && [ ! -z "$ORDER_ID" ]; then
    print_success "订单创建成功, ID: $ORDER_ID"
    echo "订单总额: $(echo $ORDER_RESPONSE | jq -r '.totalAmount')"
else
    print_error "订单创建失败"
    echo $ORDER_RESPONSE | jq .
    exit 1
fi

# 3. 创建支付记录
print_test "测试3: 创建支付记录"
PAYMENT_RESPONSE=$(curl -s -X POST "$BASE_URL/payments" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"orderId\": $ORDER_ID,
    \"paymentMethod\": \"CREDIT_CARD\",
    \"cardLastFour\": \"1234\",
    \"paymentProvider\": \"Stripe\",
    \"notes\": \"测试支付\"
  }")

PAYMENT_ID=$(echo $PAYMENT_RESPONSE | jq -r '.id')

if [ "$PAYMENT_ID" != "null" ] && [ ! -z "$PAYMENT_ID" ]; then
    print_success "支付记录创建成功, ID: $PAYMENT_ID"
    echo "支付状态: $(echo $PAYMENT_RESPONSE | jq -r '.status')"
    echo "支付金额: $(echo $PAYMENT_RESPONSE | jq -r '.amount')"
else
    print_error "支付记录创建失败"
    echo $PAYMENT_RESPONSE | jq .
    exit 1
fi

# 4. 查询支付详情
print_test "测试4: 查询支付详情"
GET_PAYMENT=$(curl -s -X GET "$BASE_URL/payments/$PAYMENT_ID" \
  -H "Authorization: Bearer $TOKEN")

PAYMENT_STATUS=$(echo $GET_PAYMENT | jq -r '.status')
print_success "查询成功，当前状态: $PAYMENT_STATUS"
echo $GET_PAYMENT | jq '{id, orderId, amount, status, paymentMethod}'

# 5. 处理支付（模拟支付流程）
print_test "测试5: 处理支付"
PROCESS_RESPONSE=$(curl -s -X POST "$BASE_URL/payments/$PAYMENT_ID/process" \
  -H "Authorization: Bearer $TOKEN")

PROCESS_STATUS=$(echo $PROCESS_RESPONSE | jq -r '.status')

if [ "$PROCESS_STATUS" == "COMPLETED" ]; then
    print_success "支付处理成功"
    echo "交易ID: $(echo $PROCESS_RESPONSE | jq -r '.transactionId')"
    echo "支付时间: $(echo $PROCESS_RESPONSE | jq -r '.paidAt')"
elif [ "$PROCESS_STATUS" == "FAILED" ]; then
    print_error "支付失败（模拟随机失败）"
    echo "失败原因: $(echo $PROCESS_RESPONSE | jq -r '.failureReason')"
else
    print_error "支付处理异常"
    echo $PROCESS_RESPONSE | jq .
fi

# 6. 根据订单ID查询支付
print_test "测试6: 根据订单ID查询支付"
ORDER_PAYMENT=$(curl -s -X GET "$BASE_URL/payments/order/$ORDER_ID" \
  -H "Authorization: Bearer $TOKEN")

ORDER_PAYMENT_ID=$(echo $ORDER_PAYMENT | jq -r '.id')

if [ "$ORDER_PAYMENT_ID" == "$PAYMENT_ID" ]; then
    print_success "根据订单查询支付成功"
    echo $ORDER_PAYMENT | jq '{id, orderId, status, amount}'
else
    print_error "查询失败"
    echo $ORDER_PAYMENT | jq .
fi

# 7. 查询我的支付历史
print_test "测试7: 查询我的支付历史"
MY_PAYMENTS=$(curl -s -X GET "$BASE_URL/payments/my-payments" \
  -H "Authorization: Bearer $TOKEN")

PAYMENT_COUNT=$(echo $MY_PAYMENTS | jq '. | length')
print_success "查询成功，共 $PAYMENT_COUNT 条支付记录"
echo $MY_PAYMENTS | jq '[.[] | {id, orderId, amount, status, createdAt}]'

# 8. 按状态查询支付
print_test "测试8: 按状态查询支付（COMPLETED）"
COMPLETED_PAYMENTS=$(curl -s -X GET "$BASE_URL/payments/my-payments/status/COMPLETED" \
  -H "Authorization: Bearer $TOKEN")

COMPLETED_COUNT=$(echo $COMPLETED_PAYMENTS | jq '. | length')
print_success "已完成的支付: $COMPLETED_COUNT 条"

# 9. 测试退款（只有COMPLETED状态才能退款）
if [ "$PROCESS_STATUS" == "COMPLETED" ]; then
    print_test "测试9: 申请部分退款"
    
    REFUND_AMOUNT="5.00"
    REFUND_RESPONSE=$(curl -s -X POST "$BASE_URL/payments/$PAYMENT_ID/refund" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d "{
        \"amount\": $REFUND_AMOUNT,
        \"reason\": \"商品质量问题\"
      }")
    
    REFUND_STATUS=$(echo $REFUND_RESPONSE | jq -r '.status')
    REFUNDED_AMOUNT=$(echo $REFUND_RESPONSE | jq -r '.refundedAmount')
    
    if [ "$REFUNDED_AMOUNT" != "null" ]; then
        print_success "退款成功"
        echo "退款金额: $REFUNDED_AMOUNT"
        echo "退款后状态: $REFUND_STATUS"
        echo "可退款余额: $(echo $REFUND_RESPONSE | jq -r '.refundableAmount')"
    else
        print_error "退款失败"
        echo $REFUND_RESPONSE | jq .
    fi
    
    # 10. 测试再次退款（剩余金额）
    print_test "测试10: 申请全额退款（退剩余部分）"
    
    REFUNDABLE=$(echo $REFUND_RESPONSE | jq -r '.refundableAmount')
    
    FULL_REFUND=$(curl -s -X POST "$BASE_URL/payments/$PAYMENT_ID/refund" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d "{
        \"amount\": $REFUNDABLE,
        \"reason\": \"完全取消订单\"
      }")
    
    FINAL_STATUS=$(echo $FULL_REFUND | jq -r '.status')
    FINAL_REFUNDED=$(echo $FULL_REFUND | jq -r '.refundedAmount')
    
    if [ "$FINAL_STATUS" == "REFUNDED" ]; then
        print_success "全额退款成功"
        echo "总退款金额: $FINAL_REFUNDED"
        echo "最终状态: $FINAL_STATUS"
    else
        print_error "全额退款失败"
        echo $FULL_REFUND | jq .
    fi
else
    print_test "测试9-10: 跳过退款测试"
    echo "原因: 支付状态为 $PROCESS_STATUS，只有COMPLETED状态才能退款"
fi

# 11. 测试重复创建支付（应该失败）
print_test "测试11: 测试订单重复支付保护"
DUPLICATE_PAYMENT=$(curl -s -X POST "$BASE_URL/payments" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"orderId\": $ORDER_ID,
    \"paymentMethod\": \"DEBIT_CARD\",
    \"cardLastFour\": \"5678\"
  }")

DUPLICATE_ID=$(echo $DUPLICATE_PAYMENT | jq -r '.id')

if [ "$DUPLICATE_ID" == "null" ] || [ -z "$DUPLICATE_ID" ]; then
    print_success "重复支付保护正常工作"
else
    print_error "重复支付保护失败！允许了重复支付"
    echo $DUPLICATE_PAYMENT | jq .
fi

# 12. 创建第二个订单测试多种支付方式
print_test "测试12: 测试数字钱包支付"
ORDER2_RESPONSE=$(curl -s -X POST "$BASE_URL/orders" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 1,
    "items": [
      {
        "menuItemId": 2,
        "quantity": 1
      }
    ],
    "deliveryAddress": "456 Oak Ave, City, State 12345"
  }')

ORDER2_ID=$(echo $ORDER2_RESPONSE | jq -r '.id')

if [ "$ORDER2_ID" != "null" ]; then
    PAYMENT2_RESPONSE=$(curl -s -X POST "$BASE_URL/payments" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d "{
        \"orderId\": $ORDER2_ID,
        \"paymentMethod\": \"DIGITAL_WALLET\",
        \"paymentProvider\": \"PayPal\"
      }")
    
    PAYMENT2_ID=$(echo $PAYMENT2_RESPONSE | jq -r '.id')
    PAYMENT2_METHOD=$(echo $PAYMENT2_RESPONSE | jq -r '.paymentMethod')
    
    if [ "$PAYMENT2_METHOD" == "DIGITAL_WALLET" ]; then
        print_success "数字钱包支付创建成功"
        echo "支付方式: $PAYMENT2_METHOD"
        echo "支付提供商: $(echo $PAYMENT2_RESPONSE | jq -r '.paymentProvider')"
    else
        print_error "数字钱包支付创建失败"
    fi
fi

# 13. 测试无效退款金额
if [ "$PROCESS_STATUS" != "COMPLETED" ]; then
    # 创建一个新的已完成支付用于测试
    print_test "测试13: 创建新订单用于退款测试"
    
    ORDER3_RESPONSE=$(curl -s -X POST "$BASE_URL/orders" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "restaurantId": 1,
        "items": [{"menuItemId": 1, "quantity": 1}],
        "deliveryAddress": "789 Pine Rd, City, State 12345"
      }')
    
    ORDER3_ID=$(echo $ORDER3_RESPONSE | jq -r '.id')
    ORDER3_AMOUNT=$(echo $ORDER3_RESPONSE | jq -r '.totalAmount')
    
    if [ "$ORDER3_ID" != "null" ]; then
        # 创建并处理支付
        PAYMENT3_RESPONSE=$(curl -s -X POST "$BASE_URL/payments" \
          -H "Authorization: Bearer $TOKEN" \
          -H "Content-Type: application/json" \
          -d "{
            \"orderId\": $ORDER3_ID,
            \"paymentMethod\": \"CREDIT_CARD\",
            \"cardLastFour\": \"9999\"
          }")
        
        PAYMENT3_ID=$(echo $PAYMENT3_RESPONSE | jq -r '.id')
        
        # 尝试处理支付直到成功（因为有10%随机失败率）
        MAX_RETRIES=5
        RETRY_COUNT=0
        while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
            PROCESS3=$(curl -s -X POST "$BASE_URL/payments/$PAYMENT3_ID/process" \
              -H "Authorization: Bearer $TOKEN")
            STATUS3=$(echo $PROCESS3 | jq -r '.status')
            
            if [ "$STATUS3" == "COMPLETED" ]; then
                break
            fi
            RETRY_COUNT=$((RETRY_COUNT + 1))
        done
        
        if [ "$STATUS3" == "COMPLETED" ]; then
            print_test "测试13: 测试无效退款金额"
            
            # 尝试退款超过支付金额
            INVALID_REFUND=$(curl -s -X POST "$BASE_URL/payments/$PAYMENT3_ID/refund" \
              -H "Authorization: Bearer $TOKEN" \
              -H "Content-Type: application/json" \
              -d "{
                \"amount\": 999999.99,
                \"reason\": \"测试超额退款\"
              }")
            
            ERROR_MSG=$(echo $INVALID_REFUND | jq -r '.message // empty')
            
            if [ ! -z "$ERROR_MSG" ] || [ "$(echo $INVALID_REFUND | jq -r '.id')" == "null" ]; then
                print_success "超额退款保护正常工作"
            else
                print_error "超额退款保护失败！"
                echo $INVALID_REFUND | jq .
            fi
        else
            print_test "测试13: 跳过（无法创建已完成的支付）"
        fi
    fi
fi

# 14. 最终统计
print_test "测试完成 - 统计信息"
FINAL_PAYMENTS=$(curl -s -X GET "$BASE_URL/payments/my-payments" \
  -H "Authorization: Bearer $TOKEN")

TOTAL_COUNT=$(echo $FINAL_PAYMENTS | jq '. | length')
COMPLETED_ONLY=$(echo $FINAL_PAYMENTS | jq '[.[] | select(.status == "COMPLETED")] | length')
REFUNDED_ONLY=$(echo $FINAL_PAYMENTS | jq '[.[] | select(.status == "REFUNDED" or .status == "PARTIALLY_REFUNDED")] | length')

echo -e "\n${GREEN}支付系统测试总结:${NC}"
echo "- 总支付记录: $TOTAL_COUNT"
echo "- 已完成支付: $COMPLETED_ONLY"
echo "- 已退款支付: $REFUNDED_ONLY"
echo -e "\n${YELLOW}详细记录:${NC}"
echo $FINAL_PAYMENTS | jq '[.[] | {id, orderId, amount, status, paymentMethod, createdAt}]'

print_success "所有测试完成！"
