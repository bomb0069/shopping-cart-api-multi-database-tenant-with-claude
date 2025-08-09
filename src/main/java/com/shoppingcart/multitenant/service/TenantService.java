package com.shoppingcart.multitenant.service;

import com.shoppingcart.multitenant.config.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class TenantService {

    private static final Logger logger = LoggerFactory.getLogger(TenantService.class);
    
    private static final List<String> AVAILABLE_TENANTS = Arrays.asList(
        "default", "tenant1", "tenant2"
    );

    public String getCurrentTenant() {
        return TenantContext.getCurrentTenant();
    }

    public boolean isValidTenant(String tenantId) {
        return tenantId != null && AVAILABLE_TENANTS.contains(tenantId);
    }

    public List<String> getAvailableTenants() {
        return AVAILABLE_TENANTS;
    }

    public void validateTenant(String tenantId) {
        if (!isValidTenant(tenantId)) {
            logger.warn("Invalid tenant ID: {}", tenantId);
            throw new IllegalArgumentException("Invalid tenant: " + tenantId);
        }
    }

    public void switchTenant(String tenantId) {
        validateTenant(tenantId);
        TenantContext.setCurrentTenant(tenantId);
        logger.info("Switched to tenant: {}", tenantId);
    }
}