# 用户登录与 JWT 验证完整指南

## 📚 目录
1. [认证流程概述](#认证流程概述)
2. [核心知识点](#核心知识点)
3. [技术架构](#技术架构)
4. [实现步骤](#实现步骤)
5. [前端集成指南](#前端集成指南)
6. [测试验证](#测试验证)

---

## 认证流程概述

### 🔐 完整的用户认证流程

```mermaid
sequenceDiagram
    participant User as 用户
    participant Frontend as 前端
    participant Backend as 后端 API
    participant Cognito as AWS Cognito
    participant DB as 数据库

    User->>Frontend: 1. 输入邮箱/密码
    Frontend->>Backend: 2. POST /api/auth/login
    Backend->>Cognito: 3. InitiateAuth (邮箱/密码)
    Cognito-->>Backend: 4. JWT Tokens (IdToken, AccessToken, RefreshToken)
    Backend->>Cognito: 5. 验证 IdToken 签名
    Cognito-->>Backend: 6. Token 有效
    Backend->>Backend: 7. 从 Token 提取用户信息 (sub, email, custom:role)
    Backend->>DB: 8. 根据 cognitoSub 查询用户
    DB-->>Backend: 9. 返回用户详情
    Backend-->>Frontend: 10. 返回 UserDTO + Tokens
    Frontend->>Frontend: 11. 存储 Tokens (localStorage/sessionStorage)
    
    Note over Frontend,Backend: 后续 API 请求
    Frontend->>Backend: 12. API 请求 + Authorization: Bearer {IdToken}
    Backend->>Backend: 13. JWT Filter 验证 Token
    Backend->>Backend: 14. 提取 custom:role 进行权限验证
    Backend->>DB: 15. 执行业务逻辑
    DB-->>Backend: 16. 返回数据
    Backend-->>Frontend: 17. 返回响应
```

### 📝 流程说明

#### 阶段 1: 用户登录
1. **用户输入** - 邮箱 + 密码
2. **前端请求** - POST `/api/auth/login`
3. **Cognito 认证** - 验证用户凭证
4. **返回 Tokens** - IdToken, AccessToken, RefreshToken

#### 阶段 2: Token 验证
5. **签名验证** - 确保 Token 未被篡改
6. **提取信息** - sub, email, custom:role
7. **数据库查询** - 获取完整用户信息

#### 阶段 3: 后续请求
8. **携带 Token** - 每次 API 请求带上 `Authorization: Bearer {token}`
9. **自动验证** - Spring Security Filter 自动验证
10. **权限控制** - 根据 `custom:role` 控制访问

---

## 核心知识点

### 1️⃣ JWT (JSON Web Token) 是什么？

#### 结构
JWT 由三部分组成，用 `.` 分隔：

```
eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ

├─ Header (头部)          ├─ Payload (负载)                  ├─ Signature (签名)
```

#### 1. Header (头部)
```json
{
  "alg": "RS256",  // 签名算法
  "typ": "JWT",    // Token 类型
  "kid": "xxxxx"   // 密钥 ID
}
```

#### 2. Payload (负载) - 存储用户信息
```json
{
  "sub": "049814d8-b051-7095-f886-abf87f8beb29",  // Cognito User Sub
  "email": "customer@example.com",
  "email_verified": true,
  "custom:role": "CUSTOMER",  // 自定义角色
  "given_name": "John",
  "family_name": "Doe",
  "iss": "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_a6gt5CsAi",
  "iat": 1737072000,  // 签发时间
  "exp": 1737075600   // 过期时间
}
```

#### 3. Signature (签名) - 防止篡改
```
RSASHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  AWS_COGNITO_PRIVATE_KEY  // AWS 的私钥，只有 AWS 拥有
)
```

### 2️⃣ AWS Cognito 的三种 Token

| Token 类型 | 用途 | 有效期 | 包含信息 |
|-----------|------|--------|---------|
| **IdToken** | 用户身份认证 | 1 小时 | 用户属性 (email, name, custom:role) |
| **AccessToken** | 访问 AWS 资源 | 1 小时 | 权限范围 (scopes) |
| **RefreshToken** | 刷新其他 Token | 30 天 | 用于获取新的 IdToken/AccessToken |

**我们主要使用 IdToken**，因为它包含用户信息和 custom:role。

### 3️⃣ Spring Security 工作原理

#### Filter Chain (过滤器链)
```
HTTP Request
    ↓
[1] SecurityContextPersistenceFilter
    ↓
[2] JwtAuthenticationFilter ← 我们自己实现
    ├─ 提取 Authorization Header
    ├─ 验证 JWT Token
    ├─ 提取用户信息 (email, role)
    └─ 设置 SecurityContext
    ↓
[3] FilterSecurityInterceptor
    ├─ 检查权限 (@PreAuthorize)
    └─ 决定是否允许访问
    ↓
Controller Method
```

#### SecurityContext
```java
// Spring Security 存储当前用户信息的地方
SecurityContextHolder.getContext()
    └─ Authentication
        ├─ Principal (用户标识，通常是 email)
        ├─ Credentials (凭证，通常为 null)
        └─ Authorities (权限列表，例如 [ROLE_CUSTOMER])
```

---

## 技术架构

### 🏗️ 项目结构

```
src/main/java/com/shydelivery/doordashsimulator/
├── config/
│   ├── CognitoConfig.java           (已有)
│   ├── SecurityConfig.java          (新增) ← Spring Security 配置
│   └── JwtConfig.java               (新增) ← JWT 相关配置
│
├── security/
│   ├── JwtAuthenticationFilter.java (新增) ← JWT 过滤器
│   ├── JwtTokenProvider.java        (新增) ← JWT 验证工具类
│   └── CognitoJwtValidator.java     (新增) ← Cognito Token 验证器
│
├── dto/
│   ├── request/
│   │   └── LoginRequest.java        (新增) ← 登录请求 DTO
│   └── response/
│       └── AuthResponse.java        (新增) ← 登录响应 DTO
│
├── controller/
│   └── AuthController.java          (新增) ← 认证控制器
│
└── service/
    ├── AuthService.java              (新增) ← 认证业务逻辑
    └── CognitoService.java           (已有) ← 已有的 Cognito 服务
```

### 🔧 所需依赖

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT 解析库 -->
<dependency>
    <groupId>com.nimbusds</groupId>
    <artifactId>nimbus-jose-jwt</artifactId>
    <version>9.37.3</version>
</dependency>

<!-- AWS Cognito Identity Provider (已有) -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>cognitoidentityprovider</artifactId>
</dependency>
```

---

## 实现步骤

### 步骤 1: 配置文件更新

#### `application.yml`
```yaml
aws:
  cognito:
    region: us-east-1
    userPoolId: us-east-1_a6gt5CsAi
    clientId: 7fv4l4ftq2qriojlfcrmu5a2d9
    # 新增：JWT 验证所需
    jwksUrl: https://cognito-idp.us-east-1.amazonaws.com/us-east-1_a6gt5CsAi/.well-known/jwks.json
    issuer: https://cognito-idp.us-east-1.amazonaws.com/us-east-1_a6gt5CsAi

# Spring Security 配置
spring:
  security:
    # 公开的接口（不需要认证）
    public-endpoints:
      - /api/auth/login
      - /api/auth/register
      - /api/users  # 注册接口
      - /error
      - /actuator/health
```

### 步骤 2: 创建 DTO 类

#### `LoginRequest.java`
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotBlank(message = "密码不能为空")
    private String password;
}
```

#### `AuthResponse.java`
```java
@Data
@Builder
public class AuthResponse {
    private String idToken;          // JWT Token (用于后续请求)
    private String accessToken;      // AWS 访问 Token
    private String refreshToken;     // 刷新 Token
    private Integer expiresIn;       // 过期时间（秒）
    private String tokenType;        // 固定为 "Bearer"
    
    // 用户信息
    private UserDTO user;
}
```

### 步骤 3: JWT 验证器

#### `CognitoJwtValidator.java`
```java
/**
 * AWS Cognito JWT Token 验证器
 * 职责：
 * 1. 下载 Cognito 的公钥 (JWKS)
 * 2. 验证 JWT 签名
 * 3. 验证 Token 是否过期
 * 4. 验证 Issuer (颁发者)
 */
@Component
@Slf4j
public class CognitoJwtValidator {
    
    @Value("${aws.cognito.jwksUrl}")
    private String jwksUrl;
    
    @Value("${aws.cognito.issuer}")
    private String issuer;
    
    private JWKSource<SecurityContext> keySource;
    
    @PostConstruct
    public void init() throws Exception {
        // 从 AWS Cognito 下载公钥
        this.keySource = JWKSourceBuilder
            .create(new URL(jwksUrl))
            .build();
    }
    
    /**
     * 验证 JWT Token
     */
    public DecodedJWT validateToken(String token) {
        // 1. 解析 Token
        // 2. 验证签名
        // 3. 验证过期时间
        // 4. 验证 Issuer
        // 5. 返回解析后的 Token
    }
    
    /**
     * 从 Token 提取用户信息
     */
    public Map<String, Object> extractClaims(DecodedJWT jwt) {
        // 提取所有 Claims (sub, email, custom:role, etc.)
    }
}
```

### 步骤 4: JWT 过滤器

#### `JwtAuthenticationFilter.java`
```java
/**
 * JWT 认证过滤器
 * 在每个 HTTP 请求前执行，验证 JWT Token
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private CognitoJwtValidator jwtValidator;
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        try {
            // 1. 从 Header 提取 Token
            String token = extractTokenFromRequest(request);
            
            if (token != null) {
                // 2. 验证 Token
                DecodedJWT jwt = jwtValidator.validateToken(token);
                
                // 3. 提取用户信息
                String cognitoSub = jwt.getSubject();
                String email = jwt.getClaim("email").asString();
                String role = jwt.getClaim("custom:role").asString();
                
                // 4. 创建 Spring Security Authentication 对象
                UserRole userRole = UserRole.valueOf(role);
                List<GrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + userRole.name())
                );
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        email,      // Principal
                        null,       // Credentials (不需要)
                        authorities // Authorities (权限)
                    );
                
                // 5. 设置到 SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            
        } catch (Exception e) {
            log.error("JWT authentication failed: {}", e.getMessage());
        }
        
        // 6. 继续过滤器链
        filterChain.doFilter(request, response);
    }
    
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);  // 去掉 "Bearer " 前缀
        }
        return null;
    }
}
```

### 步骤 5: Spring Security 配置

#### `SecurityConfig.java`
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 启用 @PreAuthorize 注解
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF (因为使用 JWT，不需要 CSRF 保护)
            .csrf(csrf -> csrf.disable())
            
            // 配置 CORS
            .cors(Customizer.withDefaults())
            
            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                // 公开接口（不需要认证）
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/users").permitAll()  // 注册接口
                .requestMatchers("/error").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            )
            
            // 配置 Session 管理（无状态）
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 添加 JWT 过滤器
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 步骤 6: 认证服务

#### `AuthService.java`
```java
@Service
@Slf4j
public class AuthService {
    
    @Autowired
    private CognitoIdentityProviderClient cognitoClient;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CognitoJwtValidator jwtValidator;
    
    @Value("${aws.cognito.userPoolId}")
    private String userPoolId;
    
    @Value("${aws.cognito.clientId}")
    private String clientId;
    
    /**
     * 用户登录
     */
    public AuthResponse login(LoginRequest request) {
        try {
            log.info("User login attempt: {}", request.getEmail());
            
            // 1. 准备认证参数
            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", request.getEmail());
            authParams.put("PASSWORD", request.getPassword());
            
            // 2. 调用 Cognito InitiateAuth
            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .clientId(clientId)
                .authParameters(authParams)
                .build();
            
            InitiateAuthResponse authResponse = cognitoClient.initiateAuth(authRequest);
            
            // 3. 获取 Tokens
            AuthenticationResultType authResult = authResponse.authenticationResult();
            String idToken = authResult.idToken();
            String accessToken = authResult.accessToken();
            String refreshToken = authResult.refreshToken();
            Integer expiresIn = authResult.expiresIn();
            
            // 4. 验证并解析 IdToken
            DecodedJWT jwt = jwtValidator.validateToken(idToken);
            String cognitoSub = jwt.getSubject();
            
            // 5. 从数据库获取用户信息
            User user = userRepository.findByCognitoSub(cognitoSub)
                .orElseThrow(() -> new BusinessException("用户不存在"));
            
            // 6. 检查用户状态
            if (!user.getIsActive()) {
                throw new BusinessException("用户已被禁用");
            }
            
            // 7. 构建响应
            UserDTO userDTO = convertToDTO(user);
            
            return AuthResponse.builder()
                .idToken(idToken)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .tokenType("Bearer")
                .user(userDTO)
                .build();
            
        } catch (NotAuthorizedException e) {
            log.error("Invalid credentials for user: {}", request.getEmail());
            throw new BusinessException("邮箱或密码错误");
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage(), e);
            throw new BusinessException("登录失败: " + e.getMessage());
        }
    }
    
    /**
     * 刷新 Token
     */
    public AuthResponse refreshToken(String refreshToken) {
        // 使用 RefreshToken 获取新的 IdToken 和 AccessToken
    }
    
    /**
     * 登出
     */
    public void logout(String accessToken) {
        // 调用 Cognito GlobalSignOut
    }
}
```

### 步骤 7: 认证控制器

#### `AuthController.java`
```java
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 刷新 Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestParam String refreshToken) {
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 登出
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);  // 去掉 "Bearer "
        authService.logout(token);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        // 从 SecurityContext 获取当前用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (String) authentication.getPrincipal();
        
        // 从数据库查询用户详情
        // ...
        
        return ResponseEntity.ok(userDTO);
    }
}
```

---

## 前端集成指南

### 🌐 前端登录流程

#### 1. 登录请求
```javascript
// 登录函数
async function login(email, password) {
    try {
        const response = await fetch('http://localhost:8080/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ email, password })
        });
        
        if (!response.ok) {
            throw new Error('登录失败');
        }
        
        const data = await response.json();
        
        // 存储 Tokens
        localStorage.setItem('idToken', data.idToken);
        localStorage.setItem('accessToken', data.accessToken);
        localStorage.setItem('refreshToken', data.refreshToken);
        localStorage.setItem('user', JSON.stringify(data.user));
        
        // 解析 JWT 获取角色
        const tokenData = parseJWT(data.idToken);
        const userRole = tokenData['custom:role'];
        
        // 根据角色跳转
        if (userRole === 'CUSTOMER') {
            window.location.href = '/customer/dashboard';
        } else if (userRole === 'RESTAURANT_OWNER') {
            window.location.href = '/restaurant/dashboard';
        } else if (userRole === 'DRIVER') {
            window.location.href = '/driver/dashboard';
        }
        
    } catch (error) {
        console.error('Login error:', error);
        alert('登录失败，请检查邮箱和密码');
    }
}

// 解析 JWT Token
function parseJWT(token) {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
        atob(base64).split('').map(c => 
            '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
        ).join('')
    );
    return JSON.parse(jsonPayload);
}
```

#### 2. 后续 API 请求（携带 Token）
```javascript
// 通用 API 请求函数
async function apiRequest(url, options = {}) {
    const idToken = localStorage.getItem('idToken');
    
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };
    
    // 添加 Authorization Header
    if (idToken) {
        headers['Authorization'] = `Bearer ${idToken}`;
    }
    
    const response = await fetch(url, {
        ...options,
        headers
    });
    
    // Token 过期处理
    if (response.status === 401) {
        // 尝试刷新 Token
        await refreshToken();
        // 重试请求
        return apiRequest(url, options);
    }
    
    return response.json();
}

// 示例：获取用户信息
async function getUserProfile() {
    const data = await apiRequest('http://localhost:8080/api/auth/me');
    return data;
}

// 示例：创建订单（需要 CUSTOMER 权限）
async function createOrder(orderData) {
    const data = await apiRequest('http://localhost:8080/api/orders', {
        method: 'POST',
        body: JSON.stringify(orderData)
    });
    return data;
}
```

#### 3. Token 刷新
```javascript
async function refreshToken() {
    const refreshToken = localStorage.getItem('refreshToken');
    
    const response = await fetch('http://localhost:8080/api/auth/refresh?refreshToken=' + refreshToken, {
        method: 'POST'
    });
    
    const data = await response.json();
    
    // 更新存储的 Tokens
    localStorage.setItem('idToken', data.idToken);
    localStorage.setItem('accessToken', data.accessToken);
    // refreshToken 可能不变
}
```

#### 4. 登出
```javascript
async function logout() {
    const idToken = localStorage.getItem('idToken');
    
    await fetch('http://localhost:8080/api/auth/logout', {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${idToken}`
        }
    });
    
    // 清除本地存储
    localStorage.removeItem('idToken');
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    
    // 跳转到登录页
    window.location.href = '/login';
}
```

#### 5. 路由保护（前端）
```javascript
// 检查用户是否已登录
function isAuthenticated() {
    const idToken = localStorage.getItem('idToken');
    if (!idToken) return false;
    
    // 检查 Token 是否过期
    const tokenData = parseJWT(idToken);
    const expirationTime = tokenData.exp * 1000;  // 转换为毫秒
    return Date.now() < expirationTime;
}

// 检查用户角色
function hasRole(requiredRole) {
    if (!isAuthenticated()) return false;
    
    const idToken = localStorage.getItem('idToken');
    const tokenData = parseJWT(idToken);
    const userRole = tokenData['custom:role'];
    
    return userRole === requiredRole;
}

// 页面加载时检查权限
window.addEventListener('DOMContentLoaded', () => {
    const currentPage = window.location.pathname;
    
    // 需要登录的页面
    if (currentPage.startsWith('/dashboard') || currentPage.startsWith('/orders')) {
        if (!isAuthenticated()) {
            window.location.href = '/login';
            return;
        }
    }
    
    // 餐厅管理页面（仅 RESTAURANT_OWNER）
    if (currentPage.startsWith('/restaurant')) {
        if (!hasRole('RESTAURANT_OWNER')) {
            alert('您没有权限访问此页面');
            window.location.href = '/';
            return;
        }
    }
    
    // 配送页面（仅 DRIVER）
    if (currentPage.startsWith('/driver')) {
        if (!hasRole('DRIVER')) {
            alert('您没有权限访问此页面');
            window.location.href = '/';
            return;
        }
    }
});
```

---

## 测试验证

### 🧪 测试步骤

#### 1. 注册用户（已完成）
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test@123",
    "firstName": "Test",
    "lastName": "User",
    "phoneNumber": "+16263805999",
    "role": "CUSTOMER"
  }'
```

#### 2. 用户登录
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test@123"
  }'
```

预期响应：
```json
{
  "idToken": "eyJraWQiOiJ...",
  "accessToken": "eyJraWQiOiJ...",
  "refreshToken": "eyJjdHkiOiJ...",
  "expiresIn": 3600,
  "tokenType": "Bearer",
  "user": {
    "id": 1,
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User",
    "role": "CUSTOMER",
    "isActive": true
  }
}
```

#### 3. 使用 Token 访问受保护的 API
```bash
# 提取 idToken (从上一步响应中复制)
TOKEN="eyJraWQiOiJ..."

# 获取当前用户信息
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

#### 4. 测试权限控制
```bash
# CUSTOMER 用户访问餐厅管理接口（应该被拒绝）
curl -X GET http://localhost:8080/api/restaurants/manage \
  -H "Authorization: Bearer $TOKEN"

# 预期响应: 403 Forbidden
```

#### 5. Token 过期测试
```bash
# 等待 Token 过期（1小时后）或使用过期的 Token
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer expired_token"

# 预期响应: 401 Unauthorized
```

#### 6. 刷新 Token
```bash
REFRESH_TOKEN="eyJjdHkiOiJ..."

curl -X POST "http://localhost:8080/api/auth/refresh?refreshToken=$REFRESH_TOKEN"
```

---

## 常见问题 FAQ

### Q1: JWT Token 存储在哪里？
**A:** 前端通常存储在：
- **localStorage** - 持久化，刷新页面不丢失（推荐用于 Web App）
- **sessionStorage** - 关闭浏览器后清除（更安全，但用户体验差）
- **Cookie** - 可设置 HttpOnly 防止 XSS 攻击（需要后端支持）

### Q2: Token 被盗用怎么办？
**A:** 
- 使用 HTTPS 加密传输
- 设置较短的过期时间（1小时）
- 实现 Token 黑名单机制
- 检测异常登录（IP、设备变化）

### Q3: 为什么不用 Session？
**A:** 
- **可扩展性** - JWT 无状态，服务器不需要存储 Session
- **微服务友好** - Token 可以在多个服务间共享
- **性能** - 不需要查询 Session 存储（Redis/DB）

### Q4: custom:role 可以被篡改吗？
**A:** 
- **不能** - JWT 有签名保护，篡改后验证会失败
- AWS 的私钥只有 AWS 拥有，无法伪造签名

### Q5: 前端如何判断用户角色？
**A:** 
```javascript
const token = localStorage.getItem('idToken');
const payload = parseJWT(token);
const role = payload['custom:role'];  // 'CUSTOMER', 'RESTAURANT_OWNER', 'DRIVER'
```

---

## 安全最佳实践

### ✅ 必须做的
1. **使用 HTTPS** - 防止 Token 被中间人窃取
2. **设置 CORS** - 限制允许的来源
3. **验证 Token 签名** - 防止伪造
4. **检查 Token 过期** - 及时刷新
5. **敏感操作二次验证** - 例如修改密码、删除账户

### ❌ 不要做的
1. **不要在 URL 中传递 Token** - 容易泄露
2. **不要在 LocalStorage 存储敏感信息** - 容易被 XSS 攻击读取
3. **不要信任前端的角色判断** - 后端必须再次验证
4. **不要使用过长的 Token 有效期** - 增加被盗用风险

---

## 实现检查清单

### 后端开发
- [ ] 添加 Spring Security 依赖
- [ ] 创建 JWT 验证器 (CognitoJwtValidator)
- [ ] 创建 JWT 过滤器 (JwtAuthenticationFilter)
- [ ] 配置 Spring Security (SecurityConfig)
- [ ] 实现登录服务 (AuthService)
- [ ] 创建认证控制器 (AuthController)
- [ ] 添加权限注解 (@PreAuthorize)
- [ ] 测试登录流程
- [ ] 测试权限控制

### 前端开发
- [ ] 创建登录页面
- [ ] 实现登录请求
- [ ] 存储 Tokens
- [ ] 实现 API 请求拦截器（自动添加 Token）
- [ ] 实现 Token 刷新逻辑
- [ ] 实现路由保护
- [ ] 根据角色显示不同界面
- [ ] 实现登出功能

### 测试
- [ ] 测试成功登录
- [ ] 测试错误密码
- [ ] 测试 Token 验证
- [ ] 测试权限控制
- [ ] 测试 Token 刷新
- [ ] 测试登出

---

## 下一步

完成用户登录后，你可以：
1. **添加权限控制** - 使用 `@PreAuthorize` 保护 API
2. **实现其他业务功能** - Restaurant, Order, Driver 的 CRUD
3. **前端集成** - 创建登录界面和各角色的 Dashboard
4. **添加更多安全特性** - 密码重置、邮箱验证、双因素认证

**准备好开始实现了吗？我们一步步来！** 🚀
