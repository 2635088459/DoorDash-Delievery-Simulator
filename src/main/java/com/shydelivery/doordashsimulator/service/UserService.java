package com.shydelivery.doordashsimulator.service;

import com.shydelivery.doordashsimulator.dto.UserDTO;
import com.shydelivery.doordashsimulator.dto.request.CreateUserRequest;
import com.shydelivery.doordashsimulator.dto.request.UpdateUserRequest;
import com.shydelivery.doordashsimulator.entity.User;
import com.shydelivery.doordashsimulator.entity.User.UserRole;
import com.shydelivery.doordashsimulator.exception.BusinessException;
import com.shydelivery.doordashsimulator.exception.ResourceNotFoundException;
import com.shydelivery.doordashsimulator.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务层
 * 处理用户相关的业务逻辑
 */
@Slf4j
@Service
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final CognitoService cognitoService;
    
    // 构造函数注入（推荐方式）
    public UserService(UserRepository userRepository, CognitoService cognitoService) {
        this.userRepository = userRepository;
        this.cognitoService = cognitoService;
    }
    
    /**
     * 创建新用户（使用 AWS Cognito）
     * 
     * @param request 创建用户请求
     * @return UserDTO
     */
    public UserDTO createUser(CreateUserRequest request) {
        log.info("Creating user with email: {}", request.getEmail());
        
        // 1. 验证邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("邮箱已存在: " + request.getEmail());
        }
        
        // 2. 验证电话号码是否已存在
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessException("电话号码已存在: " + request.getPhoneNumber());
        }
        
        // 3. 在 Cognito 中创建用户（密码由 Cognito 管理）
        String cognitoSub = cognitoService.signUpUser(request);
        
        // 4. 创建本地 User 实体（不存储密码）
        User user = new User();
        user.setEmail(request.getEmail());
        user.setCognitoSub(cognitoSub); // 链接到 Cognito
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(request.getRole());
        user.setIsActive(true); // 默认激活
        
        // 5. 保存到数据库
        User savedUser = userRepository.save(user);
        
        log.info("User created successfully with ID: {} and Cognito Sub: {}", savedUser.getId(), cognitoSub);
        
        // 6. 转换为 DTO 并返回
        return UserDTO.from(savedUser);
    }
    
    /**
     * 根据 ID 查询用户
     * 
     * @param id 用户ID
     * @return UserDTO
     */
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        log.info("Fetching user with ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        return UserDTO.from(user);
    }
    
    /**
     * 查询所有用户
     * 
     * @return List<UserDTO>
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        log.info("Fetching all users");
        
        return userRepository.findAll().stream()
                .map(UserDTO::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据角色查询用户
     * 
     * @param role 用户角色
     * @return List<UserDTO>
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getUsersByRole(UserRole role) {
        log.info("Fetching users with role: {}", role);
        
        return userRepository.findByRole(role).stream()
                .map(UserDTO::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据邮箱查询用户
     * 
     * @param email 邮箱
     * @return UserDTO
     */
    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {
        log.info("Fetching user with email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        
        return UserDTO.from(user);
    }
    
    /**
     * 搜索用户（根据名字、姓氏或邮箱）
     * 
     * @param searchTerm 搜索关键词
     * @return List<UserDTO>
     */
    @Transactional(readOnly = true)
    public List<UserDTO> searchUsers(String searchTerm) {
        log.info("Searching users with term: {}", searchTerm);
        
        return userRepository.searchUsers(searchTerm).stream()
                .map(UserDTO::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 更新用户信息
     * 
     * @param id 用户ID
     * @param request 更新请求
     * @return UserDTO
     */
    public UserDTO updateUser(Long id, UpdateUserRequest request) {
        log.info("Updating user with ID: {}", id);
        
        // 1. 查询用户
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        // 2. 更新字段（只更新非空字段）
        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            // 检查电话号码是否已被其他用户使用
            userRepository.findByPhoneNumber(request.getPhoneNumber())
                    .ifPresent(existingUser -> {
                        if (!existingUser.getId().equals(id)) {
                            throw new BusinessException("电话号码已被使用: " + request.getPhoneNumber());
                        }
                    });
            user.setPhoneNumber(request.getPhoneNumber());
        }
        
        // 3. 保存更新
        User updatedUser = userRepository.save(user);
        
        log.info("User updated successfully with ID: {}", updatedUser.getId());
        
        return UserDTO.from(updatedUser);
    }
    
    /**
     * 删除用户（同时删除 Cognito 用户）
     * 
     * @param id 用户ID
     */
    public void deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);
        
        // 1. 检查用户是否存在
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        // 2. 从 Cognito 删除用户
        if (user.getCognitoSub() != null) {
            try {
                cognitoService.deleteUser(user.getEmail());
            } catch (Exception e) {
                log.error("Failed to delete user from Cognito: {}", e.getMessage());
                // 继续删除本地用户，即使 Cognito 删除失败
            }
        }
        
        // 3. 从数据库删除
        userRepository.deleteById(id);
        
        log.info("User deleted successfully with ID: {}", id);
    }
    
    /**
     * 激活/停用用户（同时更新 Cognito 状态）
     * 
     * @param id 用户ID
     * @param isActive 是否激活
     * @return UserDTO
     */
    public UserDTO toggleUserStatus(Long id, boolean isActive) {
        log.info("Toggling user status for ID: {} to {}", id, isActive);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        // 同步更新 Cognito 用户状态
        if (user.getCognitoSub() != null) {
            try {
                if (isActive) {
                    cognitoService.enableUser(user.getEmail());
                } else {
                    cognitoService.disableUser(user.getEmail());
                }
            } catch (Exception e) {
                log.error("Failed to update user status in Cognito: {}", e.getMessage());
                throw new BusinessException("更新 Cognito 用户状态失败: " + e.getMessage());
            }
        }
        
        user.setIsActive(isActive);
        User updatedUser = userRepository.save(user);
        
        log.info("User status toggled successfully for ID: {}", id);
        
        return UserDTO.from(updatedUser);
    }
    
    /**
     * 统计用户总数
     * 
     * @return long
     */
    @Transactional(readOnly = true)
    public long countUsers() {
        return userRepository.count();
    }
    
    /**
     * 统计指定角色的用户数
     * 
     * @param role 用户角色
     * @return long
     */
    @Transactional(readOnly = true)
    public long countUsersByRole(UserRole role) {
        return userRepository.countByRole(role);
    }
}
