package com.shydelivery.doordashsimulator.service;

import com.shydelivery.doordashsimulator.dto.request.AddToCartRequest;
import com.shydelivery.doordashsimulator.dto.request.UpdateCartItemRequest;
import com.shydelivery.doordashsimulator.dto.response.CartDTO;
import com.shydelivery.doordashsimulator.dto.response.CartItemDTO;
import com.shydelivery.doordashsimulator.entity.*;
import com.shydelivery.doordashsimulator.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 购物车服务
 * 提供购物车管理功能
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CartService {
    
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    
    /**
     * 添加商品到购物车
     */
    @Transactional
    public CartDTO addToCart(Long menuItemId, AddToCartRequest request, String customerEmail) {
        log.info("Adding item to cart - menuItemId: {}, quantity: {}, customer: {}", 
                menuItemId, request.getQuantity(), customerEmail);
        
        // 获取菜品
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new IllegalArgumentException("菜品不存在，ID: " + menuItemId));
        
        // 检查菜品是否可用
        if (!menuItem.getIsAvailable()) {
            throw new IllegalStateException("菜品当前不可用: " + menuItem.getName());
        }
        
        // 获取用户
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        Restaurant restaurant = menuItem.getRestaurant();
        
        // 检查餐厅是否营业
        if (!restaurant.getIsActive()) {
            throw new IllegalStateException("餐厅当前未营业: " + restaurant.getName());
        }
        
        // 查找或创建购物车
        Cart cart = cartRepository.findByCustomerAndRestaurantAndIsActiveTrue(customer, restaurant)
                .orElseGet(() -> createCart(customer, restaurant));
        
        // 检查购物车是否过期，如果过期则续期
        if (cart.isExpired()) {
            cart.renew();
        }
        
        // 查找购物车中是否已有该商品
        CartItem existingItem = cartItemRepository.findByCartAndMenuItem(cart, menuItem)
                .orElse(null);
        
        if (existingItem != null) {
            // 如果已存在，增加数量
            existingItem.increaseQuantity(request.getQuantity());
            if (request.getSpecialInstructions() != null) {
                existingItem.setSpecialInstructions(request.getSpecialInstructions());
            }
            cartItemRepository.save(existingItem);
            log.info("Updated existing cart item quantity: {}", existingItem.getQuantity());
        } else {
            // 如果不存在，创建新商品项
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .menuItem(menuItem)
                    .quantity(request.getQuantity())
                    .priceAtAdd(menuItem.getPrice())
                    .specialInstructions(request.getSpecialInstructions())
                    .build();
            
            cart.addItem(newItem);
            cartItemRepository.save(newItem);
            log.info("Added new item to cart: {}", menuItem.getName());
        }
        
        // 更新购物车的更新时间
        cart = cartRepository.save(cart);
        
        return convertToDTO(cart);
    }
    
    /**
     * 更新购物车商品
     */
    @Transactional
    public CartDTO updateCartItem(Long cartItemId, UpdateCartItemRequest request, String customerEmail) {
        log.info("Updating cart item - cartItemId: {}, customer: {}", cartItemId, customerEmail);
        
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("购物车商品不存在，ID: " + cartItemId));
        
        // 验证购物车所有权
        if (!cartItem.getCart().getCustomer().getEmail().equals(customerEmail)) {
            throw new IllegalArgumentException("无权限修改此购物车商品");
        }
        
        // 更新数量
        if (request.getQuantity() != null) {
            cartItem.setQuantity(request.getQuantity());
        }
        
        // 更新特殊要求
        if (request.getSpecialInstructions() != null) {
            cartItem.setSpecialInstructions(request.getSpecialInstructions());
        }
        
        cartItemRepository.save(cartItem);
        Cart cart = cartRepository.save(cartItem.getCart());
        
        return convertToDTO(cart);
    }
    
    /**
     * 从购物车删除商品
     */
    @Transactional
    public CartDTO removeFromCart(Long cartItemId, String customerEmail) {
        log.info("Removing item from cart - cartItemId: {}, customer: {}", cartItemId, customerEmail);
        
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("购物车商品不存在，ID: " + cartItemId));
        
        // 验证购物车所有权
        if (!cartItem.getCart().getCustomer().getEmail().equals(customerEmail)) {
            throw new IllegalArgumentException("无权限删除此购物车商品");
        }
        
        Cart cart = cartItem.getCart();
        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);
        
        // 如果购物车为空，可以选择删除或保留购物车
        cart = cartRepository.save(cart);
        
        return convertToDTO(cart);
    }
    
    /**
     * 清空购物车
     */
    @Transactional
    public void clearCart(Long restaurantId, String customerEmail) {
        log.info("Clearing cart for restaurant: {}, customer: {}", restaurantId, customerEmail);
        
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("餐厅不存在，ID: " + restaurantId));
        
        Cart cart = cartRepository.findByCustomerAndRestaurantAndIsActiveTrue(customer, restaurant)
                .orElse(null);
        
        if (cart != null) {
            cartItemRepository.deleteByCartId(cart.getId());
            cart.clearItems();
            cartRepository.save(cart);
            log.info("Cart cleared successfully");
        }
    }
    
    /**
     * 获取用户在特定餐厅的购物车
     */
    @Transactional(readOnly = true)
    public CartDTO getCart(Long restaurantId, String customerEmail) {
        log.info("Getting cart for restaurant: {}, customer: {}", restaurantId, customerEmail);
        
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("餐厅不存在，ID: " + restaurantId));
        
        Cart cart = cartRepository.findByCustomerAndRestaurantAndIsActiveTrue(customer, restaurant)
                .orElse(null);
        
        if (cart == null || cart.isEmpty()) {
            // 返回空购物车
            return CartDTO.builder()
                    .restaurantId(restaurantId)
                    .restaurantName(restaurant.getName())
                    .items(List.of())
                    .totalItems(0)
                    .subtotal(BigDecimal.ZERO)
                    .deliveryFee(restaurant.getDeliveryFee())
                    .total(restaurant.getDeliveryFee())
                    .isActive(true)
                    .hasUnavailableItems(false)
                    .hasPriceChanges(false)
                    .build();
        }
        
        return convertToDTO(cart);
    }
    
    /**
     * 获取用户的所有购物车
     */
    @Transactional(readOnly = true)
    public List<CartDTO> getAllCarts(String customerEmail) {
        log.info("Getting all carts for customer: {}", customerEmail);
        
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        List<Cart> carts = cartRepository.findByCustomerAndIsActiveTrueOrderByUpdatedAtDesc(customer);
        
        return carts.stream()
                .filter(cart -> !cart.isEmpty())
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 定时清理过期购物车（每天凌晨2点执行）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredCarts() {
        log.info("Starting cleanup of expired carts");
        
        LocalDateTime now = LocalDateTime.now();
        List<Cart> expiredCarts = cartRepository.findExpiredCarts(now);
        
        for (Cart cart : expiredCarts) {
            cart.setIsActive(false);
            cartRepository.save(cart);
        }
        
        log.info("Cleaned up {} expired carts", expiredCarts.size());
    }
    
    /**
     * 创建新购物车
     */
    private Cart createCart(User customer, Restaurant restaurant) {
        Cart cart = Cart.builder()
                .customer(customer)
                .restaurant(restaurant)
                .isActive(true)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        
        return cartRepository.save(cart);
    }
    
    /**
     * 转换购物车实体为 DTO
     */
    private CartDTO convertToDTO(Cart cart) {
        List<CartItemDTO> itemDTOs = cart.getItems().stream()
                .map(this::convertItemToDTO)
                .collect(Collectors.toList());
        
        BigDecimal subtotal = itemDTOs.stream()
                .map(CartItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal deliveryFee = cart.getRestaurant().getDeliveryFee();
        BigDecimal total = subtotal.add(deliveryFee);
        
        boolean hasUnavailableItems = itemDTOs.stream()
                .anyMatch(item -> !item.getIsAvailable());
        
        boolean hasPriceChanges = itemDTOs.stream()
                .anyMatch(CartItemDTO::getPriceChanged);
        
        return CartDTO.builder()
                .id(cart.getId())
                .customerId(cart.getCustomer().getId())
                .restaurantId(cart.getRestaurant().getId())
                .restaurantName(cart.getRestaurant().getName())
                .items(itemDTOs)
                .totalItems(cart.getTotalItems())
                .subtotal(subtotal)
                .deliveryFee(deliveryFee)
                .total(total)
                .isActive(cart.getIsActive())
                .hasUnavailableItems(hasUnavailableItems)
                .hasPriceChanges(hasPriceChanges)
                .expiresAt(cart.getExpiresAt())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
    
    /**
     * 转换购物车商品项为 DTO
     */
    private CartItemDTO convertItemToDTO(CartItem item) {
        return CartItemDTO.builder()
                .id(item.getId())
                .menuItemId(item.getMenuItem().getId())
                .menuItemName(item.getMenuItem().getName())
                .menuItemDescription(item.getMenuItem().getDescription())
                .priceAtAdd(item.getPriceAtAdd())
                .currentPrice(item.getCurrentPrice())
                .priceChanged(item.hasPriceChanged())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .specialInstructions(item.getSpecialInstructions())
                .isAvailable(item.isMenuItemAvailable())
                .imageUrl(item.getMenuItem().getImageUrl())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
