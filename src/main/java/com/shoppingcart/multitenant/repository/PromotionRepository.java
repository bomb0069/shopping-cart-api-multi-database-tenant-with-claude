package com.shoppingcart.multitenant.repository;

import com.shoppingcart.multitenant.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    
    Optional<Promotion> findByCodeAndActiveTrue(String code);
    
    @Query("SELECT p FROM Promotion p WHERE p.active = true AND " +
           "p.validFrom <= :now AND p.validTo >= :now AND " +
           "(p.usageLimit IS NULL OR p.usageCount < p.usageLimit)")
    List<Promotion> findActivePromotions(@Param("now") LocalDateTime now);
    
    @Query("SELECT p FROM Promotion p WHERE p.active = true AND " +
           "p.validFrom <= :now AND p.validTo >= :now AND " +
           "(p.usageLimit IS NULL OR p.usageCount < p.usageLimit) AND " +
           "(p.applicableCategories IS EMPTY OR :category MEMBER OF p.applicableCategories)")
    List<Promotion> findPromotionsForCategory(@Param("category") String category,
                                            @Param("now") LocalDateTime now);
}