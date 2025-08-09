package com.shoppingcart.multitenant.repository;

import com.shoppingcart.multitenant.model.Price;
import com.shoppingcart.multitenant.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceRepository extends JpaRepository<Price, Long> {
    
    List<Price> findByProductAndActiveTrue(Product product);
    
    @Query("SELECT p FROM Price p WHERE p.product = :product AND p.active = true AND " +
           "(p.validFrom IS NULL OR p.validFrom <= :now) AND " +
           "(p.validTo IS NULL OR p.validTo >= :now) AND " +
           "(:customerGroup IS NULL OR p.customerGroup IS NULL OR p.customerGroup = :customerGroup) AND " +
           "p.minQuantity <= :quantity " +
           "ORDER BY p.customerGroup DESC NULLS LAST, p.minQuantity DESC")
    Optional<Price> findEffectivePrice(@Param("product") Product product,
                                     @Param("customerGroup") String customerGroup,
                                     @Param("quantity") Integer quantity,
                                     @Param("now") LocalDateTime now);
    
    List<Price> findByPriceTypeAndActiveTrue(String priceType);
}