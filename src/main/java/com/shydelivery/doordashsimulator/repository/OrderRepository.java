package com.shydelivery.doordashsimulator.repository;

import com.shydelivery.doordashsimulator.entity.Order;
import com.shydelivery.doordashsimulator.entity.Order.OrderStatus;
import com.shydelivery.doordashsimulator.entity.Restaurant;
import com.shydelivery.doordashsimulator.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Order Repository - Data access for Order entities
 * 
 * RBAC Context:
 * - Customers can view their own orders
 * - Restaurant owners can view orders for their restaurants
 * - Drivers can view orders assigned to them (future enhancement)
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    /**
     * Find all orders placed by a specific customer
     * Used by: CUSTOMER role to view their order history
     */
    List<Order> findByCustomer(User customer);
    
    /**
     * Find all orders placed by a customer with specific status
     * Used by: CUSTOMER role to filter orders by status
     */
    List<Order> findByCustomerAndStatus(User customer, OrderStatus status);
    
    /**
     * Find all orders for a specific restaurant
     * Used by: RESTAURANT_OWNER role to view incoming orders
     */
    List<Order> findByRestaurant(Restaurant restaurant);
    
    /**
     * Find all orders for a restaurant with specific status
     * Used by: RESTAURANT_OWNER role to filter orders (e.g., only PENDING orders)
     */
    List<Order> findByRestaurantAndStatus(Restaurant restaurant, OrderStatus status);
    
    /**
     * Find order by order number (unique identifier for customers)
     * Used by: Tracking orders by order number
     */
    Optional<Order> findByOrderNumber(String orderNumber);
    
    /**
     * Find all orders by customer ID
     * Used by: CUSTOMER role queries
     */
    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId ORDER BY o.createdAt DESC")
    List<Order> findByCustomerId(@Param("customerId") Long customerId);
    
    /**
     * Find all orders by restaurant ID
     * Used by: RESTAURANT_OWNER role queries
     */
    @Query("SELECT o FROM Order o WHERE o.restaurant.id = :restaurantId ORDER BY o.createdAt DESC")
    List<Order> findByRestaurantId(@Param("restaurantId") Long restaurantId);
    
    /**
     * Find orders by restaurant ID and status
     * Used by: RESTAURANT_OWNER to view pending/active orders
     */
    @Query("SELECT o FROM Order o WHERE o.restaurant.id = :restaurantId AND o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findByRestaurantIdAndStatus(@Param("restaurantId") Long restaurantId, @Param("status") OrderStatus status);
    
    /**
     * Check if an order exists for a specific customer
     * Used by: Ownership verification
     */
    boolean existsByIdAndCustomer(Long id, User customer);
    
    /**
     * Check if an order exists for a specific restaurant
     * Used by: Restaurant owner ownership verification
     */
    boolean existsByIdAndRestaurant(Long id, Restaurant restaurant);
    
    /**
     * Find all orders assigned to a specific driver
     * Used by: DRIVER role to view their deliveries
     */
    List<Order> findByDriver(User driver);
    
    /**
     * Find orders with specific status and no driver assigned
     * Used by: DRIVER role to find available delivery orders
     */
    List<Order> findByStatusAndDriverIsNull(OrderStatus status);
    
    /**
     * Find orders assigned to a driver with specific status
     * Used by: DRIVER role to filter their deliveries
     */
    List<Order> findByDriverAndStatus(User driver, OrderStatus status);
}
