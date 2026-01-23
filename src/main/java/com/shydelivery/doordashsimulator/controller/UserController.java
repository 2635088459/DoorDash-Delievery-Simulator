package com.shydelivery.doordashsimulator.controller;

import com.shydelivery.doordashsimulator.dto.UserDTO;
import com.shydelivery.doordashsimulator.dto.request.CreateUserRequest;
import com.shydelivery.doordashsimulator.dto.request.UpdateUserRequest;
import com.shydelivery.doordashsimulator.entity.User.UserRole;
import com.shydelivery.doordashsimulator.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 * 提供用户相关的 REST API 接口
 */
@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * 创建新用户
     * POST /api/users
     * 
     * @param request 创建用户请求
     * @return 201 Created + UserDTO
     */
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("REST request to create user: {}", request.getEmail());
        UserDTO createdUser = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }
    
    /**
     * 查询所有用户
     * GET /api/users
     * 
     * @return 200 OK + List<UserDTO>
     */
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        log.info("REST request to get all users");
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    /**
     * 根据 ID 查询用户
     * GET /api/users/{id}
     * 
     * @param id 用户ID
     * @return 200 OK + UserDTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        log.info("REST request to get user by ID: {}", id);
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    /**
     * 根据角色查询用户
     * GET /api/users/role/{role}
     * 
     * @param role 用户角色（CUSTOMER, RESTAURANT_OWNER, DRIVER）
     * @return 200 OK + List<UserDTO>
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserDTO>> getUsersByRole(@PathVariable UserRole role) {
        log.info("REST request to get users by role: {}", role);
        List<UserDTO> users = userService.getUsersByRole(role);
        return ResponseEntity.ok(users);
    }
    
    /**
     * 根据邮箱查询用户
     * GET /api/users/email/{email}
     * 
     * @param email 邮箱
     * @return 200 OK + UserDTO
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<UserDTO> getUserByEmail(@PathVariable String email) {
        log.info("REST request to get user by email: {}", email);
        UserDTO user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }
    
    /**
     * 搜索用户
     * GET /api/users/search?q=keyword
     * 
     * @param searchTerm 搜索关键词
     * @return 200 OK + List<UserDTO>
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUsers(@RequestParam("q") String searchTerm) {
        log.info("REST request to search users with term: {}", searchTerm);
        List<UserDTO> users = userService.searchUsers(searchTerm);
        return ResponseEntity.ok(users);
    }
    
    /**
     * 更新用户信息
     * PUT /api/users/{id}
     * 
     * @param id 用户ID
     * @param request 更新请求
     * @return 200 OK + UserDTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("REST request to update user with ID: {}", id);
        UserDTO updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(updatedUser);
    }
    
    /**
     * 删除用户
     * DELETE /api/users/{id}
     * 
     * @param id 用户ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("REST request to delete user with ID: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 激活/停用用户
     * PATCH /api/users/{id}/status
     * 
     * @param id 用户ID
     * @param isActive 是否激活
     * @return 200 OK + UserDTO
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserDTO> toggleUserStatus(
            @PathVariable Long id,
            @RequestParam boolean isActive) {
        log.info("REST request to toggle user status for ID: {} to {}", id, isActive);
        UserDTO user = userService.toggleUserStatus(id, isActive);
        return ResponseEntity.ok(user);
    }
    
    /**
     * 获取用户统计信息
     * GET /api/users/stats
     * 
     * @return 200 OK + 统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        log.info("REST request to get user statistics");
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userService.countUsers());
        stats.put("customerCount", userService.countUsersByRole(UserRole.CUSTOMER));
        stats.put("restaurantOwnerCount", userService.countUsersByRole(UserRole.RESTAURANT_OWNER));
        stats.put("driverCount", userService.countUsersByRole(UserRole.DRIVER));
        
        return ResponseEntity.ok(stats);
    }
}
