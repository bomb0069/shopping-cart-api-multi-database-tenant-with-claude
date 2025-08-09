package com.shoppingcart.multitenant.service;

import com.shoppingcart.multitenant.model.Promotion;
import com.shoppingcart.multitenant.repository.PromotionRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Promotion Service Tests")
class PromotionServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private PromotionService promotionService;

    private Promotion percentagePromotion;
    private Promotion fixedAmountPromotion;
    private Promotion expiredPromotion;
    private Promotion usageLimitPromotion;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        percentagePromotion = new Promotion();
        percentagePromotion.setId(1L);
        percentagePromotion.setName("10% Off");
        percentagePromotion.setCode("SAVE10");
        percentagePromotion.setDiscountType(Promotion.DiscountType.PERCENTAGE);
        percentagePromotion.setDiscountValue(new BigDecimal("10"));
        percentagePromotion.setMinOrderAmount(new BigDecimal("50"));
        percentagePromotion.setMaxDiscountAmount(new BigDecimal("20"));
        percentagePromotion.setValidFrom(now.minusDays(1));
        percentagePromotion.setValidTo(now.plusDays(30));
        percentagePromotion.setUsageCount(0);
        percentagePromotion.setActive(true);

        fixedAmountPromotion = new Promotion();
        fixedAmountPromotion.setId(2L);
        fixedAmountPromotion.setName("$15 Off");
        fixedAmountPromotion.setCode("FIXED15");
        fixedAmountPromotion.setDiscountType(Promotion.DiscountType.FIXED_AMOUNT);
        fixedAmountPromotion.setDiscountValue(new BigDecimal("15"));
        fixedAmountPromotion.setMinOrderAmount(new BigDecimal("100"));
        fixedAmountPromotion.setValidFrom(now.minusDays(1));
        fixedAmountPromotion.setValidTo(now.plusDays(30));
        fixedAmountPromotion.setUsageCount(0);
        fixedAmountPromotion.setActive(true);

        expiredPromotion = new Promotion();
        expiredPromotion.setId(3L);
        expiredPromotion.setName("Expired Promotion");
        expiredPromotion.setCode("EXPIRED");
        expiredPromotion.setDiscountType(Promotion.DiscountType.PERCENTAGE);
        expiredPromotion.setDiscountValue(new BigDecimal("20"));
        expiredPromotion.setValidFrom(now.minusDays(10));
        expiredPromotion.setValidTo(now.minusDays(1));
        expiredPromotion.setUsageCount(0);
        expiredPromotion.setActive(true);

        usageLimitPromotion = new Promotion();
        usageLimitPromotion.setId(4L);
        usageLimitPromotion.setName("Limited Use");
        usageLimitPromotion.setCode("LIMITED");
        usageLimitPromotion.setDiscountType(Promotion.DiscountType.PERCENTAGE);
        usageLimitPromotion.setDiscountValue(new BigDecimal("25"));
        usageLimitPromotion.setUsageLimit(10);
        usageLimitPromotion.setUsageCount(10);
        usageLimitPromotion.setValidFrom(now.minusDays(1));
        usageLimitPromotion.setValidTo(now.plusDays(30));
        usageLimitPromotion.setActive(true);
    }

    @Test
    @DisplayName("Should get all promotions")
    void shouldGetAllPromotions() {
        // Given
        List<Promotion> promotions = Arrays.asList(percentagePromotion, fixedAmountPromotion);
        when(promotionRepository.findAll()).thenReturn(promotions);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        List<Promotion> result = promotionService.getAllPromotions();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains(percentagePromotion, fixedAmountPromotion);
        verify(promotionRepository).findAll();
    }

    @Test
    @DisplayName("Should get active promotions")
    void shouldGetActivePromotions() {
        // Given
        List<Promotion> activePromotions = Arrays.asList(percentagePromotion, fixedAmountPromotion);
        when(promotionRepository.findActivePromotions(any(LocalDateTime.class))).thenReturn(activePromotions);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        List<Promotion> result = promotionService.getActivePromotions();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains(percentagePromotion, fixedAmountPromotion);
        verify(promotionRepository).findActivePromotions(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should get promotion by code")
    void shouldGetPromotionByCode() {
        // Given
        String code = "SAVE10";
        when(promotionRepository.findByCodeAndActiveTrue(code)).thenReturn(Optional.of(percentagePromotion));
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        Optional<Promotion> result = promotionService.getPromotionByCode(code);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(percentagePromotion);
        verify(promotionRepository).findByCodeAndActiveTrue(code);
    }

    @Test
    @DisplayName("Should create promotion with default usage count")
    void shouldCreatePromotionWithDefaultUsageCount() {
        // Given
        Promotion newPromotion = new Promotion();
        newPromotion.setName("New Promotion");
        newPromotion.setCode("NEW");
        
        when(promotionRepository.save(newPromotion)).thenReturn(newPromotion);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        Promotion result = promotionService.createPromotion(newPromotion);

        // Then
        assertThat(result).isEqualTo(newPromotion);
        assertThat(result.getUsageCount()).isEqualTo(0);
        verify(promotionRepository).save(newPromotion);
    }

    @Test
    @DisplayName("Should calculate percentage discount correctly")
    void shouldCalculatePercentageDiscountCorrectly() {
        // Given
        BigDecimal orderAmount = new BigDecimal("100.00");

        // When
        BigDecimal discount = promotionService.calculateDiscount(percentagePromotion, orderAmount);

        // Then
        assertThat(discount).isEqualTo(new BigDecimal("10.00")); // 10% of 100
    }

    @Test
    @DisplayName("Should apply maximum discount amount limit")
    void shouldApplyMaximumDiscountAmountLimit() {
        // Given
        BigDecimal orderAmount = new BigDecimal("300.00"); // Would be 30% discount, but max is 20

        // When
        BigDecimal discount = promotionService.calculateDiscount(percentagePromotion, orderAmount);

        // Then
        assertThat(discount).isEqualTo(new BigDecimal("20")); // Capped at max discount amount
    }

    @Test
    @DisplayName("Should calculate fixed amount discount")
    void shouldCalculateFixedAmountDiscount() {
        // Given
        BigDecimal orderAmount = new BigDecimal("150.00");

        // When
        BigDecimal discount = promotionService.calculateDiscount(fixedAmountPromotion, orderAmount);

        // Then
        assertThat(discount).isEqualTo(new BigDecimal("15")); // Fixed amount
    }

    @Test
    @DisplayName("Should return zero discount when order amount below minimum")
    void shouldReturnZeroDiscountWhenOrderAmountBelowMinimum() {
        // Given
        BigDecimal orderAmount = new BigDecimal("30.00"); // Below minimum of 50

        // When
        BigDecimal discount = promotionService.calculateDiscount(percentagePromotion, orderAmount);

        // Then
        assertThat(discount).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should return zero discount for expired promotion")
    void shouldReturnZeroDiscountForExpiredPromotion() {
        // Given
        BigDecimal orderAmount = new BigDecimal("100.00");

        // When
        BigDecimal discount = promotionService.calculateDiscount(expiredPromotion, orderAmount);

        // Then
        assertThat(discount).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should return zero discount when usage limit exceeded")
    void shouldReturnZeroDiscountWhenUsageLimitExceeded() {
        // Given
        BigDecimal orderAmount = new BigDecimal("100.00");

        // When
        BigDecimal discount = promotionService.calculateDiscount(usageLimitPromotion, orderAmount);

        // Then
        assertThat(discount).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should return zero discount for inactive promotion")
    void shouldReturnZeroDiscountForInactivePromotion() {
        // Given
        percentagePromotion.setActive(false);
        BigDecimal orderAmount = new BigDecimal("100.00");

        // When
        BigDecimal discount = promotionService.calculateDiscount(percentagePromotion, orderAmount);

        // Then
        assertThat(discount).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should return zero discount for null promotion")
    void shouldReturnZeroDiscountForNullPromotion() {
        // Given
        BigDecimal orderAmount = new BigDecimal("100.00");

        // When
        BigDecimal discount = promotionService.calculateDiscount(null, orderAmount);

        // Then
        assertThat(discount).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should increment promotion usage")
    void shouldIncrementPromotionUsage() {
        // Given
        Long promotionId = 1L;
        percentagePromotion.setUsageCount(5);
        
        when(promotionRepository.findById(promotionId)).thenReturn(Optional.of(percentagePromotion));
        when(promotionRepository.save(any(Promotion.class))).thenReturn(percentagePromotion);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        promotionService.incrementUsage(promotionId);

        // Then
        verify(promotionRepository).findById(promotionId);
        verify(promotionRepository).save(argThat(promotion -> promotion.getUsageCount() == 6));
    }

    @Test
    @DisplayName("Should update existing promotion")
    void shouldUpdateExistingPromotion() {
        // Given
        Long promotionId = 1L;
        Promotion updateData = new Promotion();
        updateData.setName("Updated Promotion");
        updateData.setDiscountValue(new BigDecimal("15"));
        updateData.setActive(false);
        
        when(promotionRepository.findById(promotionId)).thenReturn(Optional.of(percentagePromotion));
        when(promotionRepository.save(any(Promotion.class))).thenReturn(percentagePromotion);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        Promotion result = promotionService.updatePromotion(promotionId, updateData);

        // Then
        assertThat(result.getName()).isEqualTo("Updated Promotion");
        assertThat(result.getDiscountValue()).isEqualTo(new BigDecimal("15"));
        assertThat(result.getActive()).isFalse();
        verify(promotionRepository).findById(promotionId);
        verify(promotionRepository).save(any(Promotion.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent promotion")
    void shouldThrowExceptionWhenUpdatingNonExistentPromotion() {
        // Given
        Long promotionId = 999L;
        Promotion updateData = new Promotion();
        
        when(promotionRepository.findById(promotionId)).thenReturn(Optional.empty());
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When & Then
        assertThatThrownBy(() -> promotionService.updatePromotion(promotionId, updateData))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Promotion not found with id: " + promotionId);
        
        verify(promotionRepository).findById(promotionId);
        verify(promotionRepository, never()).save(any(Promotion.class));
    }

    @Test
    @DisplayName("Should deactivate existing promotion")
    void shouldDeactivateExistingPromotion() {
        // Given
        Long promotionId = 1L;
        when(promotionRepository.findById(promotionId)).thenReturn(Optional.of(percentagePromotion));
        when(promotionRepository.save(any(Promotion.class))).thenReturn(percentagePromotion);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        promotionService.deactivatePromotion(promotionId);

        // Then
        verify(promotionRepository).findById(promotionId);
        verify(promotionRepository).save(argThat(promotion -> !promotion.getActive()));
    }

    @Test
    @DisplayName("Should handle buy-x-get-y promotion type")
    void shouldHandleBuyXGetYPromotionType() {
        // Given
        Promotion buyXGetYPromotion = new Promotion();
        buyXGetYPromotion.setDiscountType(Promotion.DiscountType.BUY_X_GET_Y);
        buyXGetYPromotion.setDiscountValue(new BigDecimal("25"));
        buyXGetYPromotion.setValidFrom(LocalDateTime.now().minusDays(1));
        buyXGetYPromotion.setValidTo(LocalDateTime.now().plusDays(30));
        buyXGetYPromotion.setActive(true);
        buyXGetYPromotion.setUsageCount(0);
        
        BigDecimal orderAmount = new BigDecimal("100.00");

        // When
        BigDecimal discount = promotionService.calculateDiscount(buyXGetYPromotion, orderAmount);

        // Then
        assertThat(discount).isEqualTo(new BigDecimal("25")); // Uses discount value directly
    }
}