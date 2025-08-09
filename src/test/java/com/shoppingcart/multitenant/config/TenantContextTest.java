package com.shoppingcart.multitenant.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tenant Context Tests")
class TenantContextTest {

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should set and get current tenant")
    void shouldSetAndGetCurrentTenant() {
        // Given
        String tenantId = "tenant1";

        // When
        TenantContext.setCurrentTenant(tenantId);

        // Then
        assertThat(TenantContext.getCurrentTenant()).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("Should return null when no tenant is set")
    void shouldReturnNullWhenNoTenantIsSet() {
        // When & Then
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    @DisplayName("Should clear tenant context")
    void shouldClearTenantContext() {
        // Given
        TenantContext.setCurrentTenant("tenant1");
        assertThat(TenantContext.getCurrentTenant()).isEqualTo("tenant1");

        // When
        TenantContext.clear();

        // Then
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    @DisplayName("Should handle null tenant ID")
    void shouldHandleNullTenantId() {
        // Given
        TenantContext.setCurrentTenant("tenant1");
        
        // When
        TenantContext.setCurrentTenant(null);

        // Then
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    @DisplayName("Should isolate tenant context between threads")
    void shouldIsolateTenantContextBetweenThreads() throws InterruptedException {
        // Given
        String mainTenantId = "main-tenant";
        String threadTenantId = "thread-tenant";
        TenantContext.setCurrentTenant(mainTenantId);

        // When
        Thread testThread = new Thread(() -> {
            TenantContext.setCurrentTenant(threadTenantId);
            assertThat(TenantContext.getCurrentTenant()).isEqualTo(threadTenantId);
        });

        testThread.start();
        testThread.join();

        // Then
        assertThat(TenantContext.getCurrentTenant()).isEqualTo(mainTenantId);
    }
}