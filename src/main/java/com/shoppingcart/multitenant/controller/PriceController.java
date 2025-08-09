package com.shoppingcart.multitenant.controller;

import com.shoppingcart.multitenant.model.Price;
import com.shoppingcart.multitenant.service.PriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/prices")
@Validated
public class PriceController {

    @Autowired
    private PriceService priceService;

    @GetMapping("/product/{productId}/effective")
    public ResponseEntity<BigDecimal> getEffectivePrice(
            @PathVariable Long productId,
            @RequestParam(required = false) String customerGroup,
            @RequestParam(defaultValue = "1") Integer quantity) {
        try {
            BigDecimal price = priceService.getEffectivePrice(productId, customerGroup, quantity);
            return ResponseEntity.ok(price);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Price>> getProductPrices(@PathVariable Long productId) {
        try {
            List<Price> prices = priceService.getProductPrices(productId);
            return ResponseEntity.ok(prices);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/type/{priceType}")
    public ResponseEntity<List<Price>> getPricesByType(@PathVariable String priceType) {
        List<Price> prices = priceService.getPricesByType(priceType);
        return ResponseEntity.ok(prices);
    }

    @PostMapping
    public ResponseEntity<Price> createPrice(@Valid @RequestBody Price price) {
        Price createdPrice = priceService.createPrice(price);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPrice);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Price> updatePrice(
            @PathVariable Long id,
            @Valid @RequestBody Price price) {
        try {
            Price updatedPrice = priceService.updatePrice(id, price);
            return ResponseEntity.ok(updatedPrice);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrice(@PathVariable Long id) {
        try {
            priceService.deletePrice(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivatePrice(@PathVariable Long id) {
        try {
            priceService.deactivatePrice(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}