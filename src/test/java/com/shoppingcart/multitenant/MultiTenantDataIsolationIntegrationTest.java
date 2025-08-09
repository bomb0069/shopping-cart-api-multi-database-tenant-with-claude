package com.shoppingcart.multitenant;

import com.shoppingcart.multitenant.config.TenantContext;
import com.shoppingcart.multitenant.model.Product;
import com.shoppingcart.multitenant.service.ProductService;
import com.shoppingcart.multitenant.service.TenantService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Multi-Tenant Data Isolation Integration Tests")
class MultiTenantDataIsolationIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private TenantService tenantService;

    private Product tenant1Product;
    private Product tenant2Product;

    @BeforeEach
    void setUp() {
        TenantContext.clear();

        // Create test products for different tenants
        tenant1Product = new Product();
        tenant1Product.setName("Tenant 1 Product");
        tenant1Product.setDescription("Product for tenant 1");
        tenant1Product.setSku("T1-PROD-001");
        tenant1Product.setBasePrice(new BigDecimal("100.00"));
        tenant1Product.setStockQuantity(50);
        tenant1Product.setCategory("Electronics");
        tenant1Product.setBrand("Brand1");
        tenant1Product.setActive(true);

        tenant2Product = new Product();
        tenant2Product.setName("Tenant 2 Product");
        tenant2Product.setDescription("Product for tenant 2");
        tenant2Product.setSku("T2-PROD-001");
        tenant2Product.setBasePrice(new BigDecimal("200.00"));
        tenant2Product.setStockQuantity(25);
        tenant2Product.setCategory("Clothing");
        tenant2Product.setBrand("Brand2");
        tenant2Product.setActive(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should isolate data between tenants")
    void shouldIsolateDataBetweenTenants() {
        // Create product for tenant1
        tenantService.switchTenant("tenant1");
        Product savedTenant1Product = productService.createProduct(tenant1Product);
        assertThat(savedTenant1Product.getId()).isNotNull();
        assertThat(tenantService.getCurrentTenant()).isEqualTo("tenant1");

        // Verify tenant1 has 1 product
        List<Product> tenant1Products = productService.getAllActiveProducts();
        assertThat(tenant1Products).hasSize(1);
        assertThat(tenant1Products.get(0).getName()).isEqualTo("Tenant 1 Product");
        assertThat(tenant1Products.get(0).getSku()).isEqualTo("T1-PROD-001");

        // Create product for tenant2
        tenantService.switchTenant("tenant2");
        Product savedTenant2Product = productService.createProduct(tenant2Product);
        assertThat(savedTenant2Product.getId()).isNotNull();
        assertThat(tenantService.getCurrentTenant()).isEqualTo("tenant2");

        // Verify tenant2 has 1 product (different from tenant1)
        List<Product> tenant2Products = productService.getAllActiveProducts();
        assertThat(tenant2Products).hasSize(1);
        assertThat(tenant2Products.get(0).getName()).isEqualTo("Tenant 2 Product");
        assertThat(tenant2Products.get(0).getSku()).isEqualTo("T2-PROD-001");

        // Switch back to tenant1 and verify data isolation
        tenantService.switchTenant("tenant1");
        List<Product> tenant1ProductsAgain = productService.getAllActiveProducts();
        assertThat(tenant1ProductsAgain).hasSize(1);
        assertThat(tenant1ProductsAgain.get(0).getName()).isEqualTo("Tenant 1 Product");
        assertThat(tenant1ProductsAgain.get(0).getSku()).isEqualTo("T1-PROD-001");
        
        // Verify tenant1 cannot see tenant2's products
        assertThat(tenant1ProductsAgain).noneMatch(p -> p.getName().equals("Tenant 2 Product"));
    }

    @Test
    @DisplayName("Should allow same SKU across different tenants")
    void shouldAllowSameSkuAcrossDifferentTenants() {
        // Use the same SKU for both tenants
        String commonSku = "COMMON-SKU-001";
        tenant1Product.setSku(commonSku);
        tenant2Product.setSku(commonSku);

        // Create product for tenant1
        tenantService.switchTenant("tenant1");
        Product savedTenant1Product = productService.createProduct(tenant1Product);
        assertThat(savedTenant1Product.getSku()).isEqualTo(commonSku);

        // Create product with same SKU for tenant2
        tenantService.switchTenant("tenant2");
        Product savedTenant2Product = productService.createProduct(tenant2Product);
        assertThat(savedTenant2Product.getSku()).isEqualTo(commonSku);

        // Verify both tenants can have products with the same SKU
        tenantService.switchTenant("tenant1");
        assertThat(productService.getProductBySku(commonSku)).isPresent();
        assertThat(productService.getProductBySku(commonSku).get().getName()).isEqualTo("Tenant 1 Product");

        tenantService.switchTenant("tenant2");
        assertThat(productService.getProductBySku(commonSku)).isPresent();
        assertThat(productService.getProductBySku(commonSku).get().getName()).isEqualTo("Tenant 2 Product");
    }

    @Test
    @DisplayName("Should maintain separate product counts per tenant")
    void shouldMaintainSeparateProductCountsPerTenant() {
        // Create multiple products for tenant1
        tenantService.switchTenant("tenant1");
        
        Product product1 = createProductWithSku("T1-001");
        Product product2 = createProductWithSku("T1-002");
        Product product3 = createProductWithSku("T1-003");
        
        productService.createProduct(product1);
        productService.createProduct(product2);
        productService.createProduct(product3);

        List<Product> tenant1Products = productService.getAllActiveProducts();
        assertThat(tenant1Products).hasSize(3);

        // Create different number of products for tenant2
        tenantService.switchTenant("tenant2");
        
        Product product4 = createProductWithSku("T2-001");
        Product product5 = createProductWithSku("T2-002");
        
        productService.createProduct(product4);
        productService.createProduct(product5);

        List<Product> tenant2Products = productService.getAllActiveProducts();
        assertThat(tenant2Products).hasSize(2);

        // Verify counts remain separate
        tenantService.switchTenant("tenant1");
        assertThat(productService.getAllActiveProducts()).hasSize(3);

        tenantService.switchTenant("tenant2");
        assertThat(productService.getAllActiveProducts()).hasSize(2);
    }

    @Test
    @DisplayName("Should handle search isolation between tenants")
    void shouldHandleSearchIsolationBetweenTenants() {
        // Create products with similar names for different tenants
        tenantService.switchTenant("tenant1");
        Product searchableProduct1 = createProductWithName("Searchable Product Tenant1");
        productService.createProduct(searchableProduct1);

        tenantService.switchTenant("tenant2");
        Product searchableProduct2 = createProductWithName("Searchable Product Tenant2");
        productService.createProduct(searchableProduct2);

        // Search from tenant1 context
        tenantService.switchTenant("tenant1");
        var tenant1SearchResults = productService.searchProducts("Searchable", null);
        assertThat(tenant1SearchResults.getContent()).hasSize(1);
        assertThat(tenant1SearchResults.getContent().get(0).getName()).contains("Tenant1");

        // Search from tenant2 context
        tenantService.switchTenant("tenant2");
        var tenant2SearchResults = productService.searchProducts("Searchable", null);
        assertThat(tenant2SearchResults.getContent()).hasSize(1);
        assertThat(tenant2SearchResults.getContent().get(0).getName()).contains("Tenant2");
    }

    @Test
    @DisplayName("Should handle category isolation between tenants")
    void shouldHandleCategoryIsolationBetweenTenants() {
        // Create products in same category for different tenants
        String category = "TestCategory";

        tenantService.switchTenant("tenant1");
        Product categoryProduct1 = createProductWithCategory(category, "T1-CAT-001");
        productService.createProduct(categoryProduct1);

        tenantService.switchTenant("tenant2");
        Product categoryProduct2 = createProductWithCategory(category, "T2-CAT-001");
        productService.createProduct(categoryProduct2);

        // Verify category isolation
        tenantService.switchTenant("tenant1");
        List<Product> tenant1CategoryProducts = productService.getProductsByCategory(category);
        assertThat(tenant1CategoryProducts).hasSize(1);
        assertThat(tenant1CategoryProducts.get(0).getSku()).isEqualTo("T1-CAT-001");

        tenantService.switchTenant("tenant2");
        List<Product> tenant2CategoryProducts = productService.getProductsByCategory(category);
        assertThat(tenant2CategoryProducts).hasSize(1);
        assertThat(tenant2CategoryProducts.get(0).getSku()).isEqualTo("T2-CAT-001");
    }

    @Test
    @DisplayName("Should handle default tenant isolation")
    void shouldHandleDefaultTenantIsolation() {
        // Create product for default tenant
        tenantService.switchTenant("default");
        Product defaultProduct = createProductWithSku("DEFAULT-001");
        productService.createProduct(defaultProduct);

        List<Product> defaultProducts = productService.getAllActiveProducts();
        assertThat(defaultProducts).hasSize(1);

        // Create product for tenant1
        tenantService.switchTenant("tenant1");
        Product tenant1Product = createProductWithSku("T1-001");
        productService.createProduct(tenant1Product);

        List<Product> tenant1Products = productService.getAllActiveProducts();
        assertThat(tenant1Products).hasSize(1);

        // Verify isolation from default tenant
        tenantService.switchTenant("default");
        List<Product> defaultProductsAgain = productService.getAllActiveProducts();
        assertThat(defaultProductsAgain).hasSize(1);
        assertThat(defaultProductsAgain.get(0).getSku()).isEqualTo("DEFAULT-001");
    }

    private Product createProductWithSku(String sku) {
        Product product = new Product();
        product.setName("Product " + sku);
        product.setDescription("Description for " + sku);
        product.setSku(sku);
        product.setBasePrice(new BigDecimal("50.00"));
        product.setStockQuantity(10);
        product.setCategory("General");
        product.setBrand("TestBrand");
        product.setActive(true);
        return product;
    }

    private Product createProductWithName(String name) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("Description for " + name);
        product.setSku("SKU-" + System.nanoTime());
        product.setBasePrice(new BigDecimal("75.00"));
        product.setStockQuantity(15);
        product.setCategory("Search");
        product.setBrand("SearchBrand");
        product.setActive(true);
        return product;
    }

    private Product createProductWithCategory(String category, String sku) {
        Product product = new Product();
        product.setName("Product in " + category);
        product.setDescription("Product for category testing");
        product.setSku(sku);
        product.setBasePrice(new BigDecimal("25.00"));
        product.setStockQuantity(20);
        product.setCategory(category);
        product.setBrand("CategoryBrand");
        product.setActive(true);
        return product;
    }
}