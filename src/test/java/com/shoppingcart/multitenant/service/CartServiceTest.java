package com.shoppingcart.multitenant.service;

import com.shoppingcart.multitenant.model.Cart;
import com.shoppingcart.multitenant.model.CartItem;
import com.shoppingcart.multitenant.model.Product;
import com.shoppingcart.multitenant.model.Promotion;
import com.shoppingcart.multitenant.repository.CartRepository;
import com.shoppingcart.multitenant.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cart Service Tests")
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PriceService priceService;

    @Mock
    private PromotionService promotionService;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private CartService cartService;

    private Cart testCart;
    private Product testProduct;
    private CartItem testCartItem;
    private Promotion testPromotion;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setBasePrice(new BigDecimal("50.00"));
        testProduct.setStockQuantity(100);
        testProduct.setActive(true);

        testCartItem = new CartItem();
        testCartItem.setId(1L);
        testCartItem.setProduct(testProduct);
        testCartItem.setQuantity(2);
        testCartItem.setUnitPrice(new BigDecimal("50.00"));
        testCartItem.setTotalPrice(new BigDecimal("100.00"));

        testCart = new Cart();
        testCart.setId(1L);
        testCart.setSessionId("session123");
        testCart.setItems(new ArrayList<>());
        testCart.setSubtotal(BigDecimal.ZERO);
        testCart.setDiscountAmount(BigDecimal.ZERO);
        testCart.setTotalAmount(BigDecimal.ZERO);

        testPromotion = new Promotion();
        testPromotion.setId(1L);
        testPromotion.setCode("TEST10");
        testPromotion.setDiscountType(Promotion.DiscountType.PERCENTAGE);
        testPromotion.setDiscountValue(new BigDecimal("10"));
        testPromotion.setActive(true);
    }

    @Test
    @DisplayName("Should get existing cart by session ID")
    void shouldGetExistingCartBySessionId() {
        // Given
        String sessionId = "session123";
        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(testCart));
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        Cart result = cartService.getCartBySessionId(sessionId);

        // Then
        assertThat(result).isEqualTo(testCart);
        verify(cartRepository).findBySessionId(sessionId);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should create new cart when not found by session ID")
    void shouldCreateNewCartWhenNotFoundBySessionId() {
        // Given
        String sessionId = "new_session";
        Cart newCart = new Cart();
        newCart.setSessionId(sessionId);
        
        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        Cart result = cartService.getCartBySessionId(sessionId);

        // Then
        assertThat(result.getSessionId()).isEqualTo(sessionId);
        verify(cartRepository).findBySessionId(sessionId);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should add new item to empty cart")
    void shouldAddNewItemToEmptyCart() {
        // Given
        String sessionId = "session123";
        Long productId = 1L;
        Integer quantity = 2;
        BigDecimal effectivePrice = new BigDecimal("45.00");
        
        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(priceService.getEffectivePrice(productId, null, quantity)).thenReturn(effectivePrice);
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        Cart result = cartService.addItemToCart(sessionId, productId, quantity, null);

        // Then
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getSubtotal()).isEqualTo(new BigDecimal("90.00")); // 45 * 2
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("90.00"));
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should update quantity when adding existing item")
    void shouldUpdateQuantityWhenAddingExistingItem() {
        // Given
        String sessionId = "session123";
        Long productId = 1L;
        Integer quantity = 3;
        BigDecimal effectivePrice = new BigDecimal("45.00");
        
        testCartItem.setCart(testCart);
        testCart.getItems().add(testCartItem);
        
        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(priceService.getEffectivePrice(productId, null, 5)).thenReturn(effectivePrice); // 2 + 3 = 5
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        Cart result = cartService.addItemToCart(sessionId, productId, quantity, null);

        // Then
        assertThat(result.getItems()).hasSize(1);
        CartItem updatedItem = result.getItems().get(0);
        assertThat(updatedItem.getQuantity()).isEqualTo(5); // 2 + 3
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should throw exception when adding inactive product")
    void shouldThrowExceptionWhenAddingInactiveProduct() {
        // Given
        String sessionId = "session123";
        Long productId = 1L;
        Integer quantity = 1;
        testProduct.setActive(false);
        
        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When & Then
        assertThatThrownBy(() -> cartService.addItemToCart(sessionId, productId, quantity, null))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Product is not available or insufficient stock");
    }

    @Test
    @DisplayName("Should throw exception when adding more than available stock")
    void shouldThrowExceptionWhenAddingMoreThanAvailableStock() {
        // Given
        String sessionId = "session123";
        Long productId = 1L;
        Integer quantity = 150; // More than stock of 100
        testProduct.setStockQuantity(100);
        
        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When & Then
        assertThatThrownBy(() -> cartService.addItemToCart(sessionId, productId, quantity, null))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Product is not available or insufficient stock");
    }

    @Test
    @DisplayName("Should update cart item quantity")
    void shouldUpdateCartItemQuantity() {
        // Given
        String sessionId = "session123";
        Long productId = 1L;
        Integer newQuantity = 5;
        BigDecimal effectivePrice = new BigDecimal("45.00");
        
        testCartItem.setCart(testCart);
        testCart.getItems().add(testCartItem);
        
        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(testCart));
        when(priceService.getEffectivePrice(productId, null, newQuantity)).thenReturn(effectivePrice);
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        Cart result = cartService.updateCartItem(sessionId, productId, newQuantity, null);

        // Then
        assertThat(result.getItems()).hasSize(1);
        CartItem updatedItem = result.getItems().get(0);
        assertThat(updatedItem.getQuantity()).isEqualTo(5);
        assertThat(updatedItem.getTotalPrice()).isEqualTo(new BigDecimal("225.00")); // 45 * 5
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should remove item when updating quantity to zero")
    void shouldRemoveItemWhenUpdatingQuantityToZero() {
        // Given
        String sessionId = "session123";
        Long productId = 1L;
        Integer newQuantity = 0;
        
        testCartItem.setCart(testCart);
        testCart.getItems().add(testCartItem);
        
        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        Cart result = cartService.updateCartItem(sessionId, productId, newQuantity, null);

        // Then
        assertThat(result.getItems()).isEmpty();
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should remove item from cart")
    void shouldRemoveItemFromCart() {
        // Given
        String sessionId = "session123";
        Long productId = 1L;
        
        testCartItem.setCart(testCart);
        testCart.getItems().add(testCartItem);
        
        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        Cart result = cartService.removeItemFromCart(sessionId, productId);

        // Then
        assertThat(result.getItems()).isEmpty();
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should apply promotion to cart")
    void shouldApplyPromotionToCart() {
        // Given
        String sessionId = "session123";
        String promotionCode = "TEST10";
        BigDecimal subtotal = new BigDecimal("100.00");
        BigDecimal discount = new BigDecimal("10.00");
        
        testCart.setSubtotal(subtotal);
        
        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(testCart));
        when(promotionService.getPromotionByCode(promotionCode)).thenReturn(Optional.of(testPromotion));
        when(promotionService.calculateDiscount(testPromotion, subtotal)).thenReturn(discount);
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        Cart result = cartService.applyPromotion(sessionId, promotionCode);

        // Then
        assertThat(result.getAppliedPromotion()).isEqualTo(testPromotion);
        assertThat(result.getDiscountAmount()).isEqualTo(discount);
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("90.00")); // 100 - 10
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should throw exception for invalid promotion code")
    void shouldThrowExceptionForInvalidPromotionCode() {
        // Given
        String sessionId = "session123";
        String invalidCode = "INVALID";
        
        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(testCart));
        when(promotionService.getPromotionByCode(invalidCode)).thenReturn(Optional.empty());
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When & Then
        assertThatThrownBy(() -> cartService.applyPromotion(sessionId, invalidCode))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Invalid promotion code: " + invalidCode);
    }

    @Test
    @DisplayName("Should throw exception when promotion not applicable")
    void shouldThrowExceptionWhenPromotionNotApplicable() {
        // Given
        String sessionId = "session123";
        String promotionCode = "TEST10";
        BigDecimal subtotal = new BigDecimal("100.00");
        
        testCart.setSubtotal(subtotal);
        
        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(testCart));
        when(promotionService.getPromotionByCode(promotionCode)).thenReturn(Optional.of(testPromotion));
        when(promotionService.calculateDiscount(testPromotion, subtotal)).thenReturn(BigDecimal.ZERO);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When & Then
        assertThatThrownBy(() -> cartService.applyPromotion(sessionId, promotionCode))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Promotion is not applicable to this cart");
    }

    @Test
    @DisplayName("Should remove promotion from cart")
    void shouldRemovePromotionFromCart() {
        // Given
        String sessionId = "session123";
        BigDecimal subtotal = new BigDecimal("100.00");
        
        testCart.setSubtotal(subtotal);
        testCart.setAppliedPromotion(testPromotion);
        testCart.setDiscountAmount(new BigDecimal("10.00"));
        testCart.setTotalAmount(new BigDecimal("90.00"));
        
        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        Cart result = cartService.removePromotion(sessionId);

        // Then
        assertThat(result.getAppliedPromotion()).isNull();
        assertThat(result.getDiscountAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getTotalAmount()).isEqualTo(subtotal);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should clear cart")
    void shouldClearCart() {
        // Given
        String sessionId = "session123";
        testCart.getItems().add(testCartItem);
        testCart.setSubtotal(new BigDecimal("100.00"));
        testCart.setAppliedPromotion(testPromotion);
        
        when(cartRepository.findBySessionId(sessionId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        cartService.clearCart(sessionId);

        // Then
        assertThat(testCart.getItems()).isEmpty();
        assertThat(testCart.getSubtotal()).isEqualTo(BigDecimal.ZERO);
        assertThat(testCart.getDiscountAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(testCart.getTotalAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(testCart.getAppliedPromotion()).isNull();
        verify(cartRepository).save(testCart);
    }

    @Test
    @DisplayName("Should cleanup abandoned carts")
    void shouldCleanupAbandonedCarts() {
        // Given
        int daysOld = 7;
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        cartService.cleanupAbandonedCarts(daysOld);

        // Then
        verify(cartRepository).deleteAbandonedCarts(any(LocalDateTime.class));
    }
}