package com.shoppingcart.multitenant.service;

import com.shoppingcart.multitenant.model.Cart;
import com.shoppingcart.multitenant.model.CartItem;
import com.shoppingcart.multitenant.model.Product;
import com.shoppingcart.multitenant.model.Promotion;
import com.shoppingcart.multitenant.repository.CartRepository;
import com.shoppingcart.multitenant.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PriceService priceService;

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private TenantService tenantService;

    public Cart getCartBySessionId(String sessionId) {
        logger.debug("Getting cart for session {} for tenant: {}", 
                    sessionId, tenantService.getCurrentTenant());
        
        Optional<Cart> cartOpt = cartRepository.findBySessionId(sessionId);
        if (cartOpt.isEmpty()) {
            Cart newCart = new Cart();
            newCart.setSessionId(sessionId);
            return cartRepository.save(newCart);
        }
        return cartOpt.get();
    }

    public Cart getCartByUserId(String userId) {
        logger.debug("Getting cart for user {} for tenant: {}", 
                    userId, tenantService.getCurrentTenant());
        
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
        if (cartOpt.isEmpty()) {
            Cart newCart = new Cart();
            newCart.setUserId(userId);
            return cartRepository.save(newCart);
        }
        return cartOpt.get();
    }

    public Cart addItemToCart(String sessionId, Long productId, Integer quantity, String customerGroup) {
        logger.info("Adding item {} (qty: {}) to cart {} for tenant: {}", 
                   productId, quantity, sessionId, tenantService.getCurrentTenant());
        
        Cart cart = getCartBySessionId(sessionId);
        
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            throw new RuntimeException("Product not found with id: " + productId);
        }
        
        Product product = productOpt.get();
        if (!product.getActive() || product.getStockQuantity() < quantity) {
            throw new RuntimeException("Product is not available or insufficient stock");
        }

        BigDecimal effectivePrice = priceService.getEffectivePrice(productId, customerGroup, quantity);

        Optional<CartItem> existingItem = cart.getItems().stream()
            .filter(item -> item.getProduct().getId().equals(productId))
            .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;
            
            if (product.getStockQuantity() < newQuantity) {
                throw new RuntimeException("Insufficient stock for requested quantity");
            }
            
            item.setQuantity(newQuantity);
            item.setUnitPrice(effectivePrice);
            item.setTotalPrice(effectivePrice.multiply(BigDecimal.valueOf(newQuantity)));
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            newItem.setUnitPrice(effectivePrice);
            newItem.setTotalPrice(effectivePrice.multiply(BigDecimal.valueOf(quantity)));
            cart.getItems().add(newItem);
        }

        recalculateCart(cart);
        return cartRepository.save(cart);
    }

    public Cart updateCartItem(String sessionId, Long productId, Integer quantity, String customerGroup) {
        logger.info("Updating cart item {} to qty {} in cart {} for tenant: {}", 
                   productId, quantity, sessionId, tenantService.getCurrentTenant());
        
        Cart cart = getCartBySessionId(sessionId);
        
        Optional<CartItem> existingItem = cart.getItems().stream()
            .filter(item -> item.getProduct().getId().equals(productId))
            .findFirst();

        if (existingItem.isEmpty()) {
            throw new RuntimeException("Item not found in cart");
        }

        CartItem item = existingItem.get();
        Product product = item.getProduct();
        
        if (quantity <= 0) {
            cart.getItems().remove(item);
        } else {
            if (product.getStockQuantity() < quantity) {
                throw new RuntimeException("Insufficient stock for requested quantity");
            }
            
            BigDecimal effectivePrice = priceService.getEffectivePrice(productId, customerGroup, quantity);
            item.setQuantity(quantity);
            item.setUnitPrice(effectivePrice);
            item.setTotalPrice(effectivePrice.multiply(BigDecimal.valueOf(quantity)));
        }

        recalculateCart(cart);
        return cartRepository.save(cart);
    }

    public Cart removeItemFromCart(String sessionId, Long productId) {
        logger.info("Removing item {} from cart {} for tenant: {}", 
                   productId, sessionId, tenantService.getCurrentTenant());
        
        Cart cart = getCartBySessionId(sessionId);
        
        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        
        recalculateCart(cart);
        return cartRepository.save(cart);
    }

    public Cart applyPromotion(String sessionId, String promotionCode) {
        logger.info("Applying promotion {} to cart {} for tenant: {}", 
                   promotionCode, sessionId, tenantService.getCurrentTenant());
        
        Cart cart = getCartBySessionId(sessionId);
        
        Optional<Promotion> promotionOpt = promotionService.getPromotionByCode(promotionCode);
        if (promotionOpt.isEmpty()) {
            throw new RuntimeException("Invalid promotion code: " + promotionCode);
        }
        
        Promotion promotion = promotionOpt.get();
        BigDecimal discount = promotionService.calculateDiscount(promotion, cart.getSubtotal());
        
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            cart.setAppliedPromotion(promotion);
            cart.setDiscountAmount(discount);
            cart.setTotalAmount(cart.getSubtotal().subtract(discount));
        } else {
            throw new RuntimeException("Promotion is not applicable to this cart");
        }
        
        return cartRepository.save(cart);
    }

    public Cart removePromotion(String sessionId) {
        logger.info("Removing promotion from cart {} for tenant: {}", 
                   sessionId, tenantService.getCurrentTenant());
        
        Cart cart = getCartBySessionId(sessionId);
        cart.setAppliedPromotion(null);
        cart.setDiscountAmount(BigDecimal.ZERO);
        cart.setTotalAmount(cart.getSubtotal());
        
        return cartRepository.save(cart);
    }

    public void clearCart(String sessionId) {
        logger.info("Clearing cart {} for tenant: {}", sessionId, tenantService.getCurrentTenant());
        
        Cart cart = getCartBySessionId(sessionId);
        cart.getItems().clear();
        cart.setAppliedPromotion(null);
        cart.setSubtotal(BigDecimal.ZERO);
        cart.setDiscountAmount(BigDecimal.ZERO);
        cart.setTotalAmount(BigDecimal.ZERO);
        
        cartRepository.save(cart);
    }

    public void cleanupAbandonedCarts(int daysOld) {
        logger.info("Cleaning up carts older than {} days for tenant: {}", 
                   daysOld, tenantService.getCurrentTenant());
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        cartRepository.deleteAbandonedCarts(cutoffDate);
    }

    private void recalculateCart(Cart cart) {
        BigDecimal subtotal = cart.getItems().stream()
            .map(CartItem::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        cart.setSubtotal(subtotal);
        
        if (cart.getAppliedPromotion() != null) {
            BigDecimal discount = promotionService.calculateDiscount(cart.getAppliedPromotion(), subtotal);
            cart.setDiscountAmount(discount);
            cart.setTotalAmount(subtotal.subtract(discount));
        } else {
            cart.setDiscountAmount(BigDecimal.ZERO);
            cart.setTotalAmount(subtotal);
        }
    }
}