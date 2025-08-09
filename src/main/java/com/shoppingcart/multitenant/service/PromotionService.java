package com.shoppingcart.multitenant.service;

import com.shoppingcart.multitenant.model.Promotion;
import com.shoppingcart.multitenant.repository.PromotionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PromotionService {

    private static final Logger logger = LoggerFactory.getLogger(PromotionService.class);

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private TenantService tenantService;

    public List<Promotion> getAllPromotions() {
        logger.debug("Getting all promotions for tenant: {}", tenantService.getCurrentTenant());
        return promotionRepository.findAll();
    }

    public List<Promotion> getActivePromotions() {
        logger.debug("Getting active promotions for tenant: {}", tenantService.getCurrentTenant());
        return promotionRepository.findActivePromotions(LocalDateTime.now());
    }

    public List<Promotion> getPromotionsForCategory(String category) {
        logger.debug("Getting promotions for category {} for tenant: {}", 
                    category, tenantService.getCurrentTenant());
        return promotionRepository.findPromotionsForCategory(category, LocalDateTime.now());
    }

    public Optional<Promotion> getPromotionById(Long id) {
        logger.debug("Getting promotion by ID {} for tenant: {}", id, tenantService.getCurrentTenant());
        return promotionRepository.findById(id);
    }

    public Optional<Promotion> getPromotionByCode(String code) {
        logger.debug("Getting promotion by code {} for tenant: {}", code, tenantService.getCurrentTenant());
        return promotionRepository.findByCodeAndActiveTrue(code);
    }

    public Promotion createPromotion(Promotion promotion) {
        logger.info("Creating new promotion '{}' for tenant: {}", 
                   promotion.getName(), tenantService.getCurrentTenant());
        
        if (promotion.getUsageCount() == null) {
            promotion.setUsageCount(0);
        }
        
        return promotionRepository.save(promotion);
    }

    public Promotion updatePromotion(Long id, Promotion promotionUpdates) {
        logger.info("Updating promotion {} for tenant: {}", id, tenantService.getCurrentTenant());
        
        Optional<Promotion> existingPromotion = promotionRepository.findById(id);
        if (existingPromotion.isEmpty()) {
            throw new RuntimeException("Promotion not found with id: " + id);
        }

        Promotion promotion = existingPromotion.get();
        if (promotionUpdates.getName() != null) {
            promotion.setName(promotionUpdates.getName());
        }
        if (promotionUpdates.getDescription() != null) {
            promotion.setDescription(promotionUpdates.getDescription());
        }
        if (promotionUpdates.getCode() != null) {
            promotion.setCode(promotionUpdates.getCode());
        }
        if (promotionUpdates.getDiscountType() != null) {
            promotion.setDiscountType(promotionUpdates.getDiscountType());
        }
        if (promotionUpdates.getDiscountValue() != null) {
            promotion.setDiscountValue(promotionUpdates.getDiscountValue());
        }
        if (promotionUpdates.getMinOrderAmount() != null) {
            promotion.setMinOrderAmount(promotionUpdates.getMinOrderAmount());
        }
        if (promotionUpdates.getMaxDiscountAmount() != null) {
            promotion.setMaxDiscountAmount(promotionUpdates.getMaxDiscountAmount());
        }
        if (promotionUpdates.getUsageLimit() != null) {
            promotion.setUsageLimit(promotionUpdates.getUsageLimit());
        }
        if (promotionUpdates.getValidFrom() != null) {
            promotion.setValidFrom(promotionUpdates.getValidFrom());
        }
        if (promotionUpdates.getValidTo() != null) {
            promotion.setValidTo(promotionUpdates.getValidTo());
        }
        if (promotionUpdates.getApplicableProducts() != null) {
            promotion.setApplicableProducts(promotionUpdates.getApplicableProducts());
        }
        if (promotionUpdates.getApplicableCategories() != null) {
            promotion.setApplicableCategories(promotionUpdates.getApplicableCategories());
        }
        if (promotionUpdates.getActive() != null) {
            promotion.setActive(promotionUpdates.getActive());
        }

        return promotionRepository.save(promotion);
    }

    public void deletePromotion(Long id) {
        logger.info("Deleting promotion {} for tenant: {}", id, tenantService.getCurrentTenant());
        promotionRepository.deleteById(id);
    }

    public void deactivatePromotion(Long id) {
        logger.info("Deactivating promotion {} for tenant: {}", id, tenantService.getCurrentTenant());
        
        Optional<Promotion> promotion = promotionRepository.findById(id);
        if (promotion.isPresent()) {
            promotion.get().setActive(false);
            promotionRepository.save(promotion.get());
        } else {
            throw new RuntimeException("Promotion not found with id: " + id);
        }
    }

    public BigDecimal calculateDiscount(Promotion promotion, BigDecimal orderAmount) {
        if (promotion == null || !promotion.getActive()) {
            return BigDecimal.ZERO;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(promotion.getValidFrom()) || now.isAfter(promotion.getValidTo())) {
            return BigDecimal.ZERO;
        }

        if (promotion.getMinOrderAmount() != null && 
            orderAmount.compareTo(promotion.getMinOrderAmount()) < 0) {
            return BigDecimal.ZERO;
        }

        if (promotion.getUsageLimit() != null && 
            promotion.getUsageCount() >= promotion.getUsageLimit()) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;
        
        switch (promotion.getDiscountType()) {
            case PERCENTAGE:
                discount = orderAmount.multiply(promotion.getDiscountValue())
                                    .divide(BigDecimal.valueOf(100));
                break;
            case FIXED_AMOUNT:
                discount = promotion.getDiscountValue();
                break;
            case BUY_X_GET_Y:
                discount = promotion.getDiscountValue();
                break;
        }

        if (promotion.getMaxDiscountAmount() != null && 
            discount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
            discount = promotion.getMaxDiscountAmount();
        }

        return discount;
    }

    public void incrementUsage(Long promotionId) {
        logger.debug("Incrementing usage for promotion {} for tenant: {}", 
                    promotionId, tenantService.getCurrentTenant());
        
        Optional<Promotion> promotion = promotionRepository.findById(promotionId);
        if (promotion.isPresent()) {
            Promotion p = promotion.get();
            p.setUsageCount(p.getUsageCount() + 1);
            promotionRepository.save(p);
        }
    }
}