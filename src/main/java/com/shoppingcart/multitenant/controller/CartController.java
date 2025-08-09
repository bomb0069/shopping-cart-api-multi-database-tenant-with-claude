package com.shoppingcart.multitenant.controller;

import com.shoppingcart.multitenant.model.Cart;
import com.shoppingcart.multitenant.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public ResponseEntity<Cart> getCart(HttpServletRequest request) {
        String sessionId = request.getSession().getId();
        Cart cart = cartService.getCartBySessionId(sessionId);
        return ResponseEntity.ok(cart);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Cart> getCartByUserId(@PathVariable String userId) {
        Cart cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/items")
    public ResponseEntity<Cart> addItemToCart(
            @RequestParam Long productId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) String customerGroup,
            HttpServletRequest request) {
        try {
            String sessionId = request.getSession().getId();
            Cart cart = cartService.addItemToCart(sessionId, productId, quantity, customerGroup);
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/items")
    public ResponseEntity<Cart> updateCartItem(
            @RequestParam Long productId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) String customerGroup,
            HttpServletRequest request) {
        try {
            String sessionId = request.getSession().getId();
            Cart cart = cartService.updateCartItem(sessionId, productId, quantity, customerGroup);
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Cart> removeItemFromCart(
            @PathVariable Long productId,
            HttpServletRequest request) {
        String sessionId = request.getSession().getId();
        Cart cart = cartService.removeItemFromCart(sessionId, productId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/promotions/{promotionCode}")
    public ResponseEntity<Cart> applyPromotion(
            @PathVariable String promotionCode,
            HttpServletRequest request) {
        try {
            String sessionId = request.getSession().getId();
            Cart cart = cartService.applyPromotion(sessionId, promotionCode);
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/promotions")
    public ResponseEntity<Cart> removePromotion(HttpServletRequest request) {
        String sessionId = request.getSession().getId();
        Cart cart = cartService.removePromotion(sessionId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(HttpServletRequest request) {
        String sessionId = request.getSession().getId();
        cartService.clearCart(sessionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cleanup")
    public ResponseEntity<Void> cleanupAbandonedCarts(@RequestParam(defaultValue = "7") int daysOld) {
        cartService.cleanupAbandonedCarts(daysOld);
        return ResponseEntity.ok().build();
    }
}