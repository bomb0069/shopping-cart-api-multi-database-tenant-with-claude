package com.shoppingcart.multitenant.repository;

import com.shoppingcart.multitenant.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    
    Optional<Cart> findBySessionId(String sessionId);
    
    Optional<Cart> findByUserId(String userId);
    
    @Modifying
    @Query("DELETE FROM Cart c WHERE c.updatedAt < :cutoffDate")
    void deleteAbandonedCarts(@Param("cutoffDate") LocalDateTime cutoffDate);
}