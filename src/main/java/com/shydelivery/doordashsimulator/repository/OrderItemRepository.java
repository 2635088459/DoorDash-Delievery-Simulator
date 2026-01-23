package com.shydelivery.doordashsimulator.repository;

import com.shydelivery.doordashsimulator.entity.Order;
import com.shydelivery.doordashsimulator.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for OrderItem entity
 * Handles order item data access operations
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    /**
     * Find all order items for a specific order
     * 
     * @param order the order to find items for
     * @return list of order items
     */
    List<OrderItem> findByOrder(Order order);
}
