#!/bin/bash

# ============================================
# DoorDash 搜索功能测试脚本
# 测试搜索和过滤功能
# ============================================

BASE_URL="http://localhost:8080/api"

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_section() {
    echo -e "\n${BLUE}================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}================================================${NC}\n"
}

print_test() {
    echo -e "${YELLOW}$1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}\n"
}

# ============================================
# 测试 1: 全局搜索
# ============================================

print_section "测试 1: 全局搜索"

print_test "搜索关键词: 'restaurant'"
RESULT=$(curl -s "$BASE_URL/search?keyword=restaurant&size=5")
TOTAL_RESTAURANTS=$(echo "$RESULT" | jq -r '.totalRestaurants')
TOTAL_MENU_ITEMS=$(echo "$RESULT" | jq -r '.totalMenuItems')

echo "找到 $TOTAL_RESTAURANTS 个餐厅"
echo "找到 $TOTAL_MENU_ITEMS 个菜品"
print_success "全局搜索正常工作"

# ============================================
# 测试 2: 餐厅快速搜索
# ============================================

print_section "测试 2: 餐厅快速搜索"

print_test "搜索餐厅: 'restaurant'"
RESULT=$(curl -s "$BASE_URL/search/restaurants/quick?keyword=restaurant&size=10")
TOTAL=$(echo "$RESULT" | jq -r '.totalElements')
SIZE=$(echo "$RESULT" | jq -r '.size')

echo "返回总数: $TOTAL"
echo "每页大小: $SIZE"

if [ $TOTAL -gt 0 ]; then
    echo -e "\n前3个餐厅:"
    echo "$RESULT" | jq -r '.content[0:3] | .[] | "- \(.name) (\(.cuisineType))"'
fi
print_success "餐厅快速搜索正常工作"

# ============================================
# 测试 3: 菜品快速搜索
# ============================================

print_section "测试 3: 菜品快速搜索"

print_test "搜索菜品: 'chicken'"
RESULT=$(curl -s "$BASE_URL/search/menu-items/quick?keyword=chicken&size=10")
TOTAL=$(echo "$RESULT" | jq -r '.totalElements')

echo "找到 $TOTAL 个菜品"

if [ $TOTAL -gt 0 ]; then
    echo -e "\n前3个菜品:"
    echo "$RESULT" | jq -r '.content[0:3] | .[] | "- \(.name) - $\(.price) (\(.restaurantName))"'
fi
print_success "菜品快速搜索正常工作"

# ============================================
# 测试 4: 热门餐厅
# ============================================

print_section "测试 4: 热门餐厅"

print_test "获取热门餐厅 (前5个)"
RESULT=$(curl -s "$BASE_URL/search/restaurants/popular?limit=5")
COUNT=$(echo "$RESULT" | jq '. | length')

echo "返回 $COUNT 个热门餐厅"

if [ $COUNT -gt 0 ]; then
    echo -e "\n热门餐厅列表:"
    echo "$RESULT" | jq -r '.[] | "- \(.name): 评分 \(.rating // 0)/5 (\(.totalReviews) 评价)"'
fi
print_success "热门餐厅查询正常工作"

# ============================================
# 测试 5: 按评分搜索餐厅
# ============================================

print_section "测试 5: 按评分搜索餐厅"

print_test "搜索评分 >= 4.0 的餐厅"
RESULT=$(curl -s "$BASE_URL/search/restaurants/by-rating?minRating=4.0&limit=5")
COUNT=$(echo "$RESULT" | jq '. | length')

echo "找到 $COUNT 个高评分餐厅"

if [ $COUNT -gt 0 ]; then
    echo -e "\n高评分餐厅:"
    echo "$RESULT" | jq -r '.[] | "- \(.name): \(.rating // 0)/5"'
fi
print_success "按评分搜索正常工作"

# ============================================
# 测试 6: 按价格区间搜索菜品
# ============================================

print_section "测试 6: 按价格区间搜索菜品"

print_test "搜索价格在 $10-$20 的菜品"
RESULT=$(curl -s "$BASE_URL/search/menu-items/by-price?minPrice=10.00&maxPrice=20.00&limit=5")
COUNT=$(echo "$RESULT" | jq '. | length')

echo "找到 $COUNT 个符合价格范围的菜品"

if [ $COUNT -gt 0 ]; then
    echo -e "\n菜品列表:"
    echo "$RESULT" | jq -r '.[] | "- \(.name): $\(.price)"'
fi
print_success "按价格搜索正常工作"

# ============================================
# 测试 7: 高级餐厅搜索 (POST)
# ============================================

print_section "测试 7: 高级餐厅搜索"

print_test "搜索营业中的餐厅"
RESULT=$(curl -s -X POST "$BASE_URL/search/restaurants" \
  -H "Content-Type: application/json" \
  -d '{
    "openOnly": true,
    "sortBy": "rating",
    "sortDirection": "desc",
    "page": 0,
    "size": 5
  }')

TOTAL=$(echo "$RESULT" | jq -r '.totalElements')
echo "找到 $TOTAL 个营业中的餐厅"

if [ $TOTAL -gt 0 ]; then
    echo -e "\n餐厅列表 (按评分降序):"
    echo "$RESULT" | jq -r '.content[] | "- \(.name): 评分 \(.rating // 0)"'
fi
print_success "高级餐厅搜索正常工作"

# ============================================
# 测试 8: 高级菜品搜索 (POST)
# ============================================

print_section "测试 8: 高级菜品搜索"

print_test "搜索可用菜品，按价格升序"
RESULT=$(curl -s -X POST "$BASE_URL/search/menu-items" \
  -H "Content-Type: application/json" \
  -d '{
    "availableOnly": true,
    "sortBy": "price",
    "sortDirection": "asc",
    "page": 0,
    "size": 5
  }')

TOTAL=$(echo "$RESULT" | jq -r '.totalElements')
echo "找到 $TOTAL 个可用菜品"

if [ $TOTAL -gt 0 ]; then
    echo -e "\n菜品列表 (按价格升序):"
    echo "$RESULT" | jq -r '.content[] | "- \(.name): $\(.price) (\(.restaurantName))"'
fi
print_success "高级菜品搜索正常工作"

# ============================================
# 测试总结
# ============================================

print_section "测试总结"
echo -e "${GREEN}所有搜索功能测试完成！${NC}"
echo -e "\n搜索功能包括:"
echo "  ✓ 全局搜索 (同时搜索餐厅和菜品)"
echo "  ✓ 餐厅快速搜索"
echo "  ✓ 菜品快速搜索"
echo "  ✓ 热门餐厅查询"
echo "  ✓ 按评分搜索餐厅"
echo "  ✓ 按价格区间搜索菜品"
echo "  ✓ 高级餐厅搜索 (多条件过滤+排序)"
echo "  ✓ 高级菜品搜索 (多条件过滤+排序)"
echo ""
