package com.shoppingcart.multitenant.service;

import com.shoppingcart.multitenant.model.Price;
import com.shoppingcart.multitenant.model.Product;
import com.shoppingcart.multitenant.repository.PriceRepository;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Price Service Tests")
class PriceServiceTest {

    @Mock
    private PriceRepository priceRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private PriceService priceService;

    private Product testProduct;
    private Price testPrice;
    private Price specialPrice;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setBasePrice(new BigDecimal("100.00"));

        testPrice = new Price();
        testPrice.setId(1L);
        testPrice.setProduct(testProduct);
        testPrice.setPrice(new BigDecimal("90.00"));
        testPrice.setPriceType("REGULAR");
        testPrice.setMinQuantity(1);
        testPrice.setActive(true);

        specialPrice = new Price();
        specialPrice.setId(2L);
        specialPrice.setProduct(testProduct);
        specialPrice.setPrice(new BigDecimal("80.00"));
        specialPrice.setPriceType("SPECIAL");
        specialPrice.setCustomerGroup("VIP");
        specialPrice.setMinQuantity(5);
        specialPrice.setValidFrom(LocalDateTime.now().minusDays(1));
        specialPrice.setValidTo(LocalDateTime.now().plusDays(30));
        specialPrice.setActive(true);
    }

    @Test
    @DisplayName("Should get effective price using base price when no special price exists")
    void shouldGetEffectivePriceUsingBasePriceWhenNoSpecialPriceExists() {
        // Given
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(priceRepository.findEffectivePrice(any(), any(), any(), any()))
            .thenReturn(Optional.empty());
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        BigDecimal effectivePrice = priceService.getEffectivePrice(productId, null, 1);

        // Then
        assertThat(effectivePrice).isEqualTo(new BigDecimal("100.00"));
        verify(productRepository).findById(productId);
        verify(priceRepository).findEffectivePrice(
            eq(testProduct), eq(null), eq(1), any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("Should get effective price from price rule when available")
    void shouldGetEffectivePriceFromPriceRuleWhenAvailable() {
        // Given
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(priceRepository.findEffectivePrice(any(), any(), any(), any()))
            .thenReturn(Optional.of(testPrice));
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        BigDecimal effectivePrice = priceService.getEffectivePrice(productId, null, 1);

        // Then
        assertThat(effectivePrice).isEqualTo(new BigDecimal("90.00"));
        verify(productRepository).findById(productId);
        verify(priceRepository).findEffectivePrice(
            eq(testProduct), eq(null), eq(1), any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("Should get effective price for VIP customer group")
    void shouldGetEffectivePriceForVipCustomerGroup() {
        // Given
        Long productId = 1L;
        String customerGroup = "VIP";
        Integer quantity = 5;
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(priceRepository.findEffectivePrice(any(), eq(customerGroup), eq(quantity), any()))
            .thenReturn(Optional.of(specialPrice));
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        BigDecimal effectivePrice = priceService.getEffectivePrice(productId, customerGroup, quantity);

        // Then
        assertThat(effectivePrice).isEqualTo(new BigDecimal("80.00"));
        verify(productRepository).findById(productId);
        verify(priceRepository).findEffectivePrice(
            eq(testProduct), eq(customerGroup), eq(quantity), any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("Should throw exception when product not found for pricing")
    void shouldThrowExceptionWhenProductNotFoundForPricing() {
        // Given
        Long productId = 999L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When & Then
        assertThatThrownBy(() -> priceService.getEffectivePrice(productId, null, 1))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Product not found with id: " + productId);
        
        verify(productRepository).findById(productId);
        verify(priceRepository, never()).findEffectivePrice(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should get product prices")
    void shouldGetProductPrices() {
        // Given
        Long productId = 1L;
        List<Price> prices = Arrays.asList(testPrice, specialPrice);
        
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(priceRepository.findByProductAndActiveTrue(testProduct)).thenReturn(prices);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        List<Price> result = priceService.getProductPrices(productId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains(testPrice, specialPrice);
        verify(productRepository).findById(productId);
        verify(priceRepository).findByProductAndActiveTrue(testProduct);
    }

    @Test
    @DisplayName("Should get prices by type")
    void shouldGetPricesByType() {
        // Given
        String priceType = "SPECIAL";
        List<Price> prices = Arrays.asList(specialPrice);
        
        when(priceRepository.findByPriceTypeAndActiveTrue(priceType)).thenReturn(prices);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        List<Price> result = priceService.getPricesByType(priceType);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(specialPrice);
        verify(priceRepository).findByPriceTypeAndActiveTrue(priceType);
    }

    @Test
    @DisplayName("Should create price")
    void shouldCreatePrice() {
        // Given
        when(priceRepository.save(testPrice)).thenReturn(testPrice);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        Price result = priceService.createPrice(testPrice);

        // Then
        assertThat(result).isEqualTo(testPrice);
        verify(priceRepository).save(testPrice);
    }

    @Test
    @DisplayName("Should update existing price")
    void shouldUpdateExistingPrice() {
        // Given
        Long priceId = 1L;
        Price updateData = new Price();
        updateData.setPrice(new BigDecimal("85.00"));
        updateData.setPriceType("DISCOUNT");
        updateData.setActive(false);
        
        when(priceRepository.findById(priceId)).thenReturn(Optional.of(testPrice));
        when(priceRepository.save(any(Price.class))).thenReturn(testPrice);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        Price result = priceService.updatePrice(priceId, updateData);

        // Then
        assertThat(result.getPrice()).isEqualTo(new BigDecimal("85.00"));
        assertThat(result.getPriceType()).isEqualTo("DISCOUNT");
        assertThat(result.getActive()).isFalse();
        verify(priceRepository).findById(priceId);
        verify(priceRepository).save(any(Price.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent price")
    void shouldThrowExceptionWhenUpdatingNonExistentPrice() {
        // Given
        Long priceId = 999L;
        Price updateData = new Price();
        
        when(priceRepository.findById(priceId)).thenReturn(Optional.empty());
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When & Then
        assertThatThrownBy(() -> priceService.updatePrice(priceId, updateData))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Price not found with id: " + priceId);
        
        verify(priceRepository).findById(priceId);
        verify(priceRepository, never()).save(any(Price.class));
    }

    @Test
    @DisplayName("Should delete price")
    void shouldDeletePrice() {
        // Given
        Long priceId = 1L;
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        priceService.deletePrice(priceId);

        // Then
        verify(priceRepository).deleteById(priceId);
    }

    @Test
    @DisplayName("Should deactivate existing price")
    void shouldDeactivateExistingPrice() {
        // Given
        Long priceId = 1L;
        when(priceRepository.findById(priceId)).thenReturn(Optional.of(testPrice));
        when(priceRepository.save(any(Price.class))).thenReturn(testPrice);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        priceService.deactivatePrice(priceId);

        // Then
        verify(priceRepository).findById(priceId);
        verify(priceRepository).save(argThat(price -> !price.getActive()));
    }

    @Test
    @DisplayName("Should throw exception when deactivating non-existent price")
    void shouldThrowExceptionWhenDeactivatingNonExistentPrice() {
        // Given
        Long priceId = 999L;
        when(priceRepository.findById(priceId)).thenReturn(Optional.empty());
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When & Then
        assertThatThrownBy(() -> priceService.deactivatePrice(priceId))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Price not found with id: " + priceId);
        
        verify(priceRepository).findById(priceId);
        verify(priceRepository, never()).save(any(Price.class));
    }
}