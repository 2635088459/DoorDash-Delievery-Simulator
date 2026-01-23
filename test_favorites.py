import requests
import json

BASE_URL = "http://localhost:8080/api"

# 登录
login_response = requests.post(
    f"{BASE_URL}/auth/login",
    json={"email": "carttest@example.com", "password": "Password123!"}
)

print("Login status:", login_response.status_code)
if login_response.status_code == 200:
    token = login_response.json()["accessToken"]
    print("Token obtained:", token[:50] + "...")
    
    # 测试收藏餐厅列表
    headers = {"Authorization": f"Bearer {token}"}
    fav_response = requests.get(f"{BASE_URL}/favorites/restaurants", headers=headers)
    
    print(f"\nFavorites endpoint status: {fav_response.status_code}")
    print(f"Response: {fav_response.text[:200]}")
    
    # 测试收藏餐厅 ID=2
    add_response = requests.post(
        f"{BASE_URL}/favorites/restaurants/2",
        headers=headers,
        json={"note": "测试收藏"}
    )
    
    print(f"\nAdd favorite status: {add_response.status_code}")
    print(f"Response: {add_response.text[:200]}")
else:
    print("Login failed:", login_response.text)
