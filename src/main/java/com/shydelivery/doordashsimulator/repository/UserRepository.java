package com.shydelivery.doordashsimulator.repository;

import com.shydelivery.doordashsimulator.entity.User;
import com.shydelivery.doordashsimulator.entity.User.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity
 * Provides database access methods for User operations
 * 
 * 继承 JpaRepository 自动提供以下方法：
 * - save(entity)          保存/更新用户
 * - findById(id)          按ID查询用户
 * - findAll()             查询所有用户
 * - deleteById(id)        按ID删除用户
 * - count()               统计用户数量
 * - existsById(id)        判断用户是否存在
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // ==================== 基础查询方法 ====================
    
    /**
     * 根据邮箱查询用户
     * 用于登录验证和检查邮箱是否已存在
     * 
     * @param email 用户邮箱
     * @return Optional<User> 用户对象（可能不存在）
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 根据 Cognito Sub 查询用户
     * 用于通过 JWT Token 获取用户信息
     * 
     * @param cognitoSub AWS Cognito User Sub
     * @return Optional<User> 用户对象（可能不存在）
     */
    Optional<User> findByCognitoSub(String cognitoSub);
    
    /**
     * 检查邮箱是否已存在
     * 用于注册时验证邮箱唯一性
     * 
     * @param email 用户邮箱
     * @return true 如果邮箱已存在
     */
    boolean existsByEmail(String email);
    
    /**
     * 根据电话号码查询用户
     * 
     * @param phoneNumber 电话号码
     * @return Optional<User> 用户对象（可能不存在）
     */
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    /**
     * 检查电话号码是否已存在
     * 
     * @param phoneNumber 电话号码
     * @return true 如果电话号码已存在
     */
    boolean existsByPhoneNumber(String phoneNumber);
    
    // ==================== 按角色查询 ====================
    
    /**
     * 查询指定角色的所有用户
     * 
     * @param role 用户角色（CUSTOMER, RESTAURANT_OWNER, DRIVER）
     * @return List<User> 该角色的所有用户
     */
    List<User> findByRole(UserRole role);
    
    /**
     * 统计指定角色的用户数量
     * 
     * @param role 用户角色
     * @return long 用户数量
     */
    long countByRole(UserRole role);
    
    /**
     * 查询指定角色且账号激活的用户
     * 
     * @param role 用户角色
     * @param isActive 是否激活
     * @return List<User> 符合条件的用户列表
     */
    List<User> findByRoleAndIsActive(UserRole role, Boolean isActive);
    
    // ==================== 按激活状态查询 ====================
    
    /**
     * 查询所有激活的用户
     * 
     * @param isActive 是否激活
     * @return List<User> 激活的用户列表
     */
    List<User> findByIsActive(Boolean isActive);
    
    /**
     * 统计激活的用户数量
     * 
     * @param isActive 是否激活
     * @return long 用户数量
     */
    long countByIsActive(Boolean isActive);
    
    // ==================== 模糊查询 ====================
    
    /**
     * 根据名字模糊查询用户
     * 
     * @param firstName 名字（支持部分匹配）
     * @return List<User> 匹配的用户列表
     */
    List<User> findByFirstNameContaining(String firstName);
    
    /**
     * 根据姓氏模糊查询用户
     * 
     * @param lastName 姓氏（支持部分匹配）
     * @return List<User> 匹配的用户列表
     */
    List<User> findByLastNameContaining(String lastName);
    
    /**
     * 根据名字或姓氏模糊查询用户
     * 
     * @param firstName 名字
     * @param lastName 姓氏
     * @return List<User> 匹配的用户列表
     */
    List<User> findByFirstNameContainingOrLastNameContaining(String firstName, String lastName);
    
    // ==================== 自定义 JPQL 查询 ====================
    
    /**
     * 根据邮箱查询用户（自定义查询示例）
     * 使用 JPQL 查询语言
     * 
     * @param email 用户邮箱
     * @return Optional<User> 用户对象
     */
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailCustom(@Param("email") String email);
    
    /**
     * 查询指定角色的激活用户，按创建时间倒序排列
     * 
     * @param role 用户角色
     * @param isActive 是否激活
     * @return List<User> 用户列表
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = :isActive ORDER BY u.createdAt DESC")
    List<User> findActiveUsersByRole(@Param("role") UserRole role, @Param("isActive") Boolean isActive);
    
    /**
     * 搜索用户：根据名字、姓氏或邮箱模糊匹配
     * 
     * @param searchTerm 搜索关键词
     * @return List<User> 匹配的用户列表
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchUsers(@Param("searchTerm") String searchTerm);
    
    /**
     * 统计每个角色的用户数量
     * 返回结果示例：[{role: "CUSTOMER", count: 100}, {role: "DRIVER", count: 50}]
     * 
     * @return List<Object[]> 每个元素包含 [UserRole, Long]
     */
    @Query("SELECT u.role, COUNT(u) FROM User u GROUP BY u.role")
    List<Object[]> countUsersByRole();
    
    // ==================== 原生 SQL 查询（示例）====================
    
    /**
     * 使用原生 SQL 查询用户
     * 注意：原生 SQL 直接操作数据库表，不推荐常用
     * 
     * @param email 用户邮箱
     * @return Optional<User> 用户对象
     */
    @Query(value = "SELECT * FROM users WHERE email = ?1", nativeQuery = true)
    Optional<User> findByEmailNative(String email);
}
