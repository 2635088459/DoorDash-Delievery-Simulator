package com.shydelivery.doordashsimulator.repository;

import com.shydelivery.doordashsimulator.entity.Address;
import com.shydelivery.doordashsimulator.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Address Repository - Data access for Address entities
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    
    /**
     * Find all addresses for a specific user
     */
    List<Address> findByUser(User user);
    
    /**
     * Find user's default address
     */
    Optional<Address> findByUserAndIsDefaultTrue(User user);
    
    /**
     * Find addresses by user ID
     */
    List<Address> findByUserId(Long userId);
}
