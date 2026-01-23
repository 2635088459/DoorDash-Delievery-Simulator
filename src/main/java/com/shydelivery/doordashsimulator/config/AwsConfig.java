package com.shydelivery.doordashsimulator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

/**
 * AWS 服务配置
 * 
 * 配置 AWS SDK Clients：
 * - CognitoIdentityProviderClient：用于用户认证和管理
 */
@Configuration
public class AwsConfig {
    
    @Value("${aws.cognito.region}")
    private String region;
    
    /**
     * 配置 AWS Cognito Identity Provider Client
     * 
     * 用于：
     * - 用户登录（InitiateAuth）
     * - Token 刷新（InitiateAuth with REFRESH_TOKEN_AUTH）
     * - 用户登出（GlobalSignOut）
     * - 用户管理（AdminGetUser, AdminCreateUser, etc.）
     */
    @Bean
    public CognitoIdentityProviderClient cognitoIdentityProviderClient() {
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
