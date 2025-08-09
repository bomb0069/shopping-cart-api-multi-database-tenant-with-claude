package com.shoppingcart.multitenant;

import com.shoppingcart.multitenant.config.TenantContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

@TestConfiguration
@Profile("test")
public class TestConfiguration {

    @Bean
    @Primary
    public DataSource testDataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .setName("testdb")
            .build();
    }

    /**
     * Test utility bean to help with tenant context management in tests
     */
    @Bean
    public TestTenantContextHelper testTenantContextHelper() {
        return new TestTenantContextHelper();
    }

    public static class TestTenantContextHelper {

        public void setTenant(String tenantId) {
            TenantContext.setCurrentTenant(tenantId);
        }

        public String getCurrentTenant() {
            return TenantContext.getCurrentTenant();
        }

        public void clearTenant() {
            TenantContext.clear();
        }

        public void withTenant(String tenantId, Runnable operation) {
            String originalTenant = TenantContext.getCurrentTenant();
            try {
                TenantContext.setCurrentTenant(tenantId);
                operation.run();
            } finally {
                if (originalTenant != null) {
                    TenantContext.setCurrentTenant(originalTenant);
                } else {
                    TenantContext.clear();
                }
            }
        }
    }
}