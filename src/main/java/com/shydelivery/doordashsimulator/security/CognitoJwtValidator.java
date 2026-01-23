package com.shydelivery.doordashsimulator.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * AWS Cognito JWT Token 验证器
 * 
 * 职责：
 * 1. 从 AWS Cognito 下载公钥 (JWKS)
 * 2. 验证 JWT 签名
 * 3. 验证 Token 是否过期
 * 4. 验证 Issuer (颁发者)
 * 5. 提取 Token 中的用户信息
 */
@Component
@Slf4j
public class CognitoJwtValidator {
    
    @Value("${aws.cognito.jwksUrl}")
    private String jwksUrl;
    
    @Value("${aws.cognito.issuer}")
    private String expectedIssuer;
    
    @Value("${aws.cognito.region}")
    private String region;
    
    @Value("${aws.cognito.userPoolId}")
    private String userPoolId;
    
    private Map<String, RSAKey> keyMap = new HashMap<>();
    
    /**
     * 初始化：从 AWS Cognito 下载公钥
     */
    @PostConstruct
    public void init() {
        try {
            log.info("Initializing Cognito JWT Validator...");
            log.info("JWKS URL: {}", jwksUrl);
            log.info("Expected Issuer: {}", expectedIssuer);
            
            // 从 AWS Cognito 下载 JWKS (JSON Web Key Set)
            JWKSet jwkSet = JWKSet.load(new URL(jwksUrl));
            
            // 将所有公钥存储到 Map 中（key = kid）
            jwkSet.getKeys().forEach(jwk -> {
                if (jwk instanceof RSAKey) {
                    RSAKey rsaKey = (RSAKey) jwk;
                    keyMap.put(rsaKey.getKeyID(), rsaKey);
                    log.info("Loaded RSA key with kid: {}", rsaKey.getKeyID());
                }
            });
            
            log.info("Successfully loaded {} keys from Cognito JWKS", keyMap.size());
            
        } catch (Exception e) {
            log.error("Failed to initialize Cognito JWT Validator: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load Cognito public keys", e);
        }
    }
    
    /**
     * 验证 JWT Token
     * 
     * @param token JWT Token 字符串
     * @return 解析后的 SignedJWT 对象
     * @throws Exception 如果验证失败
     */
    public SignedJWT validateToken(String token) throws Exception {
        try {
            // 1. 解析 JWT Token
            SignedJWT signedJWT = SignedJWT.parse(token);
            
            // 2. 获取 Token Header 中的 kid (Key ID)
            String kid = signedJWT.getHeader().getKeyID();
            if (kid == null) {
                throw new SecurityException("Token header missing 'kid' field");
            }
            
            // 3. 根据 kid 获取对应的公钥
            RSAKey rsaKey = keyMap.get(kid);
            if (rsaKey == null) {
                log.error("No public key found for kid: {}", kid);
                throw new SecurityException("Invalid token: no matching public key");
            }
            
            // 4. 验证签名
            JWSVerifier verifier = new RSASSAVerifier(rsaKey);
            if (!signedJWT.verify(verifier)) {
                throw new SecurityException("Invalid token signature");
            }
            
            // 5. 验证过期时间
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expirationTime == null || expirationTime.before(new Date())) {
                throw new SecurityException("Token has expired");
            }
            
            // 6. 验证 Issuer (颁发者)
            String issuer = signedJWT.getJWTClaimsSet().getIssuer();
            if (!expectedIssuer.equals(issuer)) {
                log.error("Invalid issuer. Expected: {}, Got: {}", expectedIssuer, issuer);
                throw new SecurityException("Invalid token issuer");
            }
            
            // 7. 验证 Token Use (应该是 "id" 表示 ID Token)
            String tokenUse = (String) signedJWT.getJWTClaimsSet().getClaim("token_use");
            if (!"id".equals(tokenUse)) {
                log.warn("Token use is not 'id': {}", tokenUse);
            }
            
            log.debug("Token validation successful for subject: {}", signedJWT.getJWTClaimsSet().getSubject());
            return signedJWT;
            
        } catch (ParseException e) {
            log.error("Failed to parse JWT token: {}", e.getMessage());
            throw new SecurityException("Invalid token format", e);
        } catch (JOSEException e) {
            log.error("Failed to verify JWT signature: {}", e.getMessage());
            throw new SecurityException("Token signature verification failed", e);
        }
    }
    
    /**
     * 从 Token 提取所有 Claims
     * 
     * @param signedJWT 已验证的 JWT Token
     * @return Claims Map
     */
    public Map<String, Object> extractClaims(SignedJWT signedJWT) {
        try {
            return signedJWT.getJWTClaimsSet().getClaims();
        } catch (ParseException e) {
            log.error("Failed to extract claims from JWT: {}", e.getMessage());
            throw new RuntimeException("Failed to extract claims", e);
        }
    }
    
    /**
     * 从 Token 提取用户 Sub (Cognito User ID)
     */
    public String extractCognitoSub(SignedJWT signedJWT) {
        try {
            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (ParseException e) {
            log.error("Failed to extract subject from JWT: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从 Token 提取用户邮箱
     */
    public String extractEmail(SignedJWT signedJWT) {
        try {
            return signedJWT.getJWTClaimsSet().getStringClaim("email");
        } catch (ParseException e) {
            log.error("Failed to extract email from JWT: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从 Token 提取用户角色
     */
    public String extractRole(SignedJWT signedJWT) {
        try {
            return signedJWT.getJWTClaimsSet().getStringClaim("custom:role");
        } catch (ParseException e) {
            log.error("Failed to extract role from JWT: {}", e.getMessage());
            return null;
        }
    }
}
