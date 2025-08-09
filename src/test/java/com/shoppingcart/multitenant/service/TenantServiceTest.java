package com.shoppingcart.multitenant.service;

import com.shoppingcart.multitenant.config.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tenant Service Tests")
class TenantServiceTest {

    @InjectMocks
    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should return current tenant from context")
    void shouldReturnCurrentTenantFromContext() {
        // Given
        String expectedTenant = "tenant1";
        TenantContext.setCurrentTenant(expectedTenant);

        // When
        String currentTenant = tenantService.getCurrentTenant();

        // Then
        assertThat(currentTenant).isEqualTo(expectedTenant);
    }

    @Test
    @DisplayName("Should validate valid tenant IDs")
    void shouldValidateValidTenantIds() {
        // Given & When & Then
        assertThat(tenantService.isValidTenant("default")).isTrue();
        assertThat(tenantService.isValidTenant("tenant1")).isTrue();
        assertThat(tenantService.isValidTenant("tenant2")).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid tenant IDs")
    void shouldRejectInvalidTenantIds() {
        // Given & When & Then
        assertThat(tenantService.isValidTenant("invalid")).isFalse();
        assertThat(tenantService.isValidTenant("")).isFalse();
        assertThat(tenantService.isValidTenant(null)).isFalse();
        assertThat(tenantService.isValidTenant("tenant3")).isFalse();
    }

    @Test
    @DisplayName("Should return available tenants list")
    void shouldReturnAvailableTenantsList() {
        // When
        List<String> availableTenants = tenantService.getAvailableTenants();

        // Then
        assertThat(availableTenants)
            .isNotNull()
            .isNotEmpty()
            .contains("default", "tenant1", "tenant2")
            .hasSize(3);
    }

    @Test
    @DisplayName("Should validate tenant successfully for valid tenant")
    void shouldValidateTenantSuccessfullyForValidTenant() {
        // Given
        String validTenant = "tenant1";

        // When & Then (should not throw exception)
        tenantService.validateTenant(validTenant);
    }

    @Test
    @DisplayName("Should throw exception for invalid tenant")
    void shouldThrowExceptionForInvalidTenant() {
        // Given
        String invalidTenant = "invalid-tenant";

        // When & Then
        assertThatThrownBy(() -> tenantService.validateTenant(invalidTenant))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid tenant: " + invalidTenant);
    }

    @Test
    @DisplayName("Should throw exception for null tenant")
    void shouldThrowExceptionForNullTenant() {
        // When & Then
        assertThatThrownBy(() -> tenantService.validateTenant(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid tenant: null");
    }

    @Test
    @DisplayName("Should switch tenant successfully")
    void shouldSwitchTenantSuccessfully() {
        // Given
        String newTenant = "tenant2";

        // When
        tenantService.switchTenant(newTenant);

        // Then
        assertThat(TenantContext.getCurrentTenant()).isEqualTo(newTenant);
    }

    @Test
    @DisplayName("Should throw exception when switching to invalid tenant")
    void shouldThrowExceptionWhenSwitchingToInvalidTenant() {
        // Given
        String invalidTenant = "invalid";

        // When & Then
        assertThatThrownBy(() -> tenantService.switchTenant(invalidTenant))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid tenant: " + invalidTenant);
    }
}