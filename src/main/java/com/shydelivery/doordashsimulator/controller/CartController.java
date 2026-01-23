package com.shydelivery.doordashsimulator.controller;

import com.shydelivery.doordashsimulator.dto.request.AddToCartRequest;
import com.shydelivery.doordashsimulator.dto.request.UpdateCartItemRequest;
import com.shydelivery.doordashsimulator.dto.response.CartDTO;
import com.shydelivery.doordashsimulator.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * 购物车控制器
 * 提供购物车管理接口
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {
    
    private final CartService cartService;
    
    /**
     * 添加商品到购物车
     */
    @PostMapping("/items/{menuItemId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<CartDTO> addToCart(
            @PathVariable Long menuItemId,
            @Valid @RequestBody AddToCartRequest request,
            Principal principal) {
        
        String customerEmail = principal.getName();
        log.info("Adding item to cart - menuItemId: {}, customer: {}", menuItemId, customerEmail);
        
        CartDTO cart = cartService.addToCart(menuItemId, request, customerEmail);
        return ResponseEntity.ok(cart);
    }
    
    /**
     * 更新购物车商品
     */
    @PutMapping("/items/{cartItemId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<CartDTO> updateCartItem(
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            Principal principal) {
        
        String customerEmail = principal.getName();
        log.info("Updating cart item - cartItemId: {}, customer: {}", cartItemId, customerEmail);
        
        CartDTO cart = cartService.updateCartItem(cartItemId, request, customerEmail);
        return ResponseEntity.ok(cart);
    }
    
    /**
     * 从购物车删除商品
     */
    @DeleteMapping("/items/{cartItemId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<CartDTO> removeFromCart(
            @PathVariable Long cartItemId,
            Principal principal) {
        
        String customerEmail = principal.getName();
        log.info("Removing item from cart - cartItemId: {}, customer: {}", cartItemId, customerEmail);
        
        CartDTO cart = cartService.removeFromCart(cartItemId, customerEmail);
        return ResponseEntity.ok(cart);
    }
    
    /**
     * 清空购物车
     */
    @DeleteMapping("/{restaurantId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Void> clearCart(
            @PathVariable Long restaurantId,
            Principal principal) {
        
        String customerEmail = principal.getName();
        log.info("Clearing cart - restaurantId: {}, customer: {}", restaurantId, customerEmail);
        
        cartService.clearCart(restaurantId, customerEmail);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 获取用户在特定餐厅的购物车
     */
    @GetMapping("/{restaurantId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<CartDTO> getCart(
            @PathVariable Long restaurantId,
            Principal principal) {
        
        String customerEmail = principal.getName();
        log.info("Getting cart - restaurantId: {}, customer: {}", restaurantId, customerEmail);
        
        CartDTO cart = cartService.getCart(restaurantId, customerEmail);
        return ResponseEntity.ok(cart);
    }
    
    /**
     * 获取用户的所有购物车
     */
    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<List<CartDTO>> getAllCarts(Principal principal) {
        
        String customerEmail = principal.getName();
        log.info("Getting all carts for customer: {}", customerEmail);
        
        List<CartDTO> carts = cartService.getAllCarts(customerEmail);
        return ResponseEntity.ok(carts);
    }
}
