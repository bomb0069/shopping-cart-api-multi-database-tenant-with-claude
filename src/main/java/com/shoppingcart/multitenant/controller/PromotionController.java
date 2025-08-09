package com.shoppingcart.multitenant.controller;

import com.shoppingcart.multitenant.model.Promotion;
import com.shoppingcart.multitenant.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/promotions")
@Validated
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    @GetMapping
    public ResponseEntity<List<Promotion>> getAllPromotions() {
        List<Promotion> promotions = promotionService.getAllPromotions();
        return ResponseEntity.ok(promotions);
    }

    @GetMapping("/active")
    public ResponseEntity<List<Promotion>> getActivePromotions() {
        List<Promotion> promotions = promotionService.getActivePromotions();
        return ResponseEntity.ok(promotions);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Promotion>> getPromotionsForCategory(@PathVariable String category) {
        List<Promotion> promotions = promotionService.getPromotionsForCategory(category);
        return ResponseEntity.ok(promotions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Promotion> getPromotionById(@PathVariable Long id) {
        Optional<Promotion> promotion = promotionService.getPromotionById(id);
        return promotion.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<Promotion> getPromotionByCode(@PathVariable String code) {
        Optional<Promotion> promotion = promotionService.getPromotionByCode(code);
        return promotion.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/calculate-discount")
    public ResponseEntity<BigDecimal> calculateDiscount(
            @PathVariable Long id,
            @RequestParam BigDecimal orderAmount) {
        Optional<Promotion> promotion = promotionService.getPromotionById(id);
        if (promotion.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        BigDecimal discount = promotionService.calculateDiscount(promotion.get(), orderAmount);
        return ResponseEntity.ok(discount);
    }

    @PostMapping("/code/{code}/calculate-discount")
    public ResponseEntity<BigDecimal> calculateDiscountByCode(
            @PathVariable String code,
            @RequestParam BigDecimal orderAmount) {
        Optional<Promotion> promotion = promotionService.getPromotionByCode(code);
        if (promotion.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        BigDecimal discount = promotionService.calculateDiscount(promotion.get(), orderAmount);
        return ResponseEntity.ok(discount);
    }

    @PostMapping
    public ResponseEntity<Promotion> createPromotion(@Valid @RequestBody Promotion promotion) {
        Promotion createdPromotion = promotionService.createPromotion(promotion);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPromotion);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Promotion> updatePromotion(
            @PathVariable Long id,
            @Valid @RequestBody Promotion promotion) {
        try {
            Promotion updatedPromotion = promotionService.updatePromotion(id, promotion);
            return ResponseEntity.ok(updatedPromotion);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromotion(@PathVariable Long id) {
        try {
            promotionService.deletePromotion(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivatePromotion(@PathVariable Long id) {
        try {
            promotionService.deactivatePromotion(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/increment-usage")
    public ResponseEntity<Void> incrementUsage(@PathVariable Long id) {
        promotionService.incrementUsage(id);
        return ResponseEntity.ok().build();
    }
}