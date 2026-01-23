package com.shydelivery.doordashsimulator.service;

import com.shydelivery.doordashsimulator.dto.request.CreateUserRequest;
import com.shydelivery.doordashsimulator.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AWS Cognito Service
 * Handles all interactions with AWS Cognito User Pool
 */
@Slf4j
@Service
public class CognitoService {

    private final CognitoIdentityProviderClient cognitoClient;

    @Value("${aws.cognito.userPoolId}")
    private String userPoolId;

    @Value("${aws.cognito.clientId}")
    private String clientId;

    public CognitoService(CognitoIdentityProviderClient cognitoClient) {
        this.cognitoClient = cognitoClient;
    }

    /**
     * 在 Cognito 中注册新用户
     *
     * @param request 用户注册请求
     * @return Cognito User Sub (unique identifier)
     */
    public String signUpUser(CreateUserRequest request) {
        try {
            log.info("Creating user in Cognito: {}", request.getEmail());

            // 为了避免 email alias 冲突，使用 UUID 作为 username
            String username = "user_" + UUID.randomUUID().toString().substring(0, 8);

            // 准备用户属性
            // 注意: custom:role 需要先在 AWS Cognito User Pool 中配置自定义属性
            // 配置路径: Cognito Console -> User Pool -> Sign-up experience -> Attributes -> Add custom attribute
            Map<String, String> attributes = new HashMap<>();
            attributes.put("email", request.getEmail());
            attributes.put("given_name", request.getFirstName());
            attributes.put("family_name", request.getLastName());
            attributes.put("phone_number", request.getPhoneNumber());
            attributes.put("custom:role", request.getRole().toString());

            // 创建 Cognito 用户
            AdminCreateUserRequest cognitoRequest = AdminCreateUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)  // 使用 UUID 而不是 email
                    .temporaryPassword(request.getPassword())
                    .userAttributes(
                            AttributeType.builder().name("email").value(request.getEmail()).build(),
                            AttributeType.builder().name("email_verified").value("true").build(),
                            AttributeType.builder().name("given_name").value(request.getFirstName()).build(),
                            AttributeType.builder().name("family_name").value(request.getLastName()).build(),
                            AttributeType.builder().name("phone_number").value(request.getPhoneNumber()).build(),
                            AttributeType.builder().name("custom:role").value(request.getRole().toString()).build()
                    )
                    .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                    .build();

            AdminCreateUserResponse response = cognitoClient.adminCreateUser(cognitoRequest);

            // 获取用户的 Sub (unique identifier)
            String createdUsername = response.user().username();
            
            // 设置永久密码（可选，如果希望用户首次登录时修改密码，可以跳过这步）
            AdminSetUserPasswordRequest setPasswordRequest = AdminSetUserPasswordRequest.builder()
                    .userPoolId(userPoolId)
                    .username(createdUsername)
                    .password(request.getPassword())
                    .permanent(true)
                    .build();

            cognitoClient.adminSetUserPassword(setPasswordRequest);

            log.info("User created successfully in Cognito: {}", createdUsername);

            // 返回用户的 Sub
            return getUserSub(createdUsername);

        } catch (UsernameExistsException e) {
            log.error("User already exists in Cognito: {}", request.getEmail());
            throw new BusinessException("用户已存在: " + request.getEmail());
        } catch (Exception e) {
            log.error("Failed to create user in Cognito: {}", e.getMessage(), e);
            throw new BusinessException("创建用户失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的 Cognito Sub
     *
     * @param username 用户名（通常是邮箱）
     * @return Cognito Sub
     */
    public String getUserSub(String username) {
        try {
            AdminGetUserRequest request = AdminGetUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build();

            AdminGetUserResponse response = cognitoClient.adminGetUser(request);

            // 从用户属性中获取 sub
            return response.userAttributes().stream()
                    .filter(attr -> "sub".equals(attr.name()))
                    .findFirst()
                    .map(AttributeType::value)
                    .orElseThrow(() -> new BusinessException("无法获取用户 Sub"));

        } catch (Exception e) {
            log.error("Failed to get user sub: {}", e.getMessage(), e);
            throw new BusinessException("获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 从 Cognito 删除用户
     *
     * @param username 用户名
     */
    public void deleteUser(String username) {
        try {
            log.info("Deleting user from Cognito: {}", username);

            AdminDeleteUserRequest request = AdminDeleteUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build();

            cognitoClient.adminDeleteUser(request);

            log.info("User deleted successfully from Cognito: {}", username);

        } catch (Exception e) {
            log.error("Failed to delete user from Cognito: {}", e.getMessage(), e);
            throw new BusinessException("删除用户失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户属性
     *
     * @param username 用户名
     * @param attributes 要更新的属性
     */
    public void updateUserAttributes(String username, Map<String, String> attributes) {
        try {
            log.info("Updating user attributes in Cognito: {}", username);

            AttributeType[] attributeTypes = attributes.entrySet().stream()
                    .map(entry -> AttributeType.builder()
                            .name(entry.getKey())
                            .value(entry.getValue())
                            .build())
                    .toArray(AttributeType[]::new);

            AdminUpdateUserAttributesRequest request = AdminUpdateUserAttributesRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .userAttributes(attributeTypes)
                    .build();

            cognitoClient.adminUpdateUserAttributes(request);

            log.info("User attributes updated successfully in Cognito: {}", username);

        } catch (Exception e) {
            log.error("Failed to update user attributes in Cognito: {}", e.getMessage(), e);
            throw new BusinessException("更新用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 禁用用户
     *
     * @param username 用户名
     */
    public void disableUser(String username) {
        try {
            log.info("Disabling user in Cognito: {}", username);

            AdminDisableUserRequest request = AdminDisableUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build();

            cognitoClient.adminDisableUser(request);

            log.info("User disabled successfully in Cognito: {}", username);

        } catch (Exception e) {
            log.error("Failed to disable user in Cognito: {}", e.getMessage(), e);
            throw new BusinessException("禁用用户失败: " + e.getMessage());
        }
    }

    /**
     * 启用用户
     *
     * @param username 用户名
     */
    public void enableUser(String username) {
        try {
            log.info("Enabling user in Cognito: {}", username);

            AdminEnableUserRequest request = AdminEnableUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build();

            cognitoClient.adminEnableUser(request);

            log.info("User enabled successfully in Cognito: {}", username);

        } catch (Exception e) {
            log.error("Failed to enable user in Cognito: {}", e.getMessage(), e);
            throw new BusinessException("启用用户失败: " + e.getMessage());
        }
    }

    /**
     * 验证访问令牌（用于 API 认证）
     *
     * @param accessToken JWT 访问令牌
     * @return 用户的 Cognito Sub
     */
    public String validateToken(String accessToken) {
        try {
            GetUserRequest request = GetUserRequest.builder()
                    .accessToken(accessToken)
                    .build();

            GetUserResponse response = cognitoClient.getUser(request);

            return response.userAttributes().stream()
                    .filter(attr -> "sub".equals(attr.name()))
                    .findFirst()
                    .map(AttributeType::value)
                    .orElseThrow(() -> new BusinessException("无效的访问令牌"));

        } catch (Exception e) {
            log.error("Failed to validate token: {}", e.getMessage());
            throw new BusinessException("令牌验证失败: " + e.getMessage());
        }
    }
}
