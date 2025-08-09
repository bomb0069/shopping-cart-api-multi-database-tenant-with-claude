package com.shoppingcart.multitenant.config;

import com.shoppingcart.multitenant.model.Product;
import com.shoppingcart.multitenant.model.Price;
import com.shoppingcart.multitenant.model.Promotion;
import com.shoppingcart.multitenant.service.ProductService;
import com.shoppingcart.multitenant.service.PriceService;
import com.shoppingcart.multitenant.service.PromotionService;
import com.shoppingcart.multitenant.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    @Autowired
    private TenantService tenantService;

    @Autowired
    private ProductService productService;

    @Autowired
    private PriceService priceService;

    @Autowired
    private PromotionService promotionService;

    @Override
    public void run(String... args) throws Exception {
        List<String> tenants = Arrays.asList("tenant1", "tenant2");
        
        for (String tenant : tenants) {
            logger.info("Loading sample data for tenant: {}", tenant);
            tenantService.switchTenant(tenant);
            loadSampleData();
        }
    }

    private void loadSampleData() {
        Product product1 = new Product();
        product1.setName("Sample Laptop");
        product1.setDescription("High-performance laptop for professionals");
        product1.setSku("SKU-LAPTOP-001");
        product1.setBasePrice(new BigDecimal("999.99"));
        product1.setStockQuantity(50);
        product1.setCategory("Electronics");
        product1.setBrand("TechBrand");
        product1.setImageUrls(Arrays.asList("laptop1.jpg", "laptop2.jpg"));
        product1 = productService.createProduct(product1);

        Product product2 = new Product();
        product2.setName("Wireless Mouse");
        product2.setDescription("Ergonomic wireless mouse");
        product2.setSku("SKU-MOUSE-001");
        product2.setBasePrice(new BigDecimal("29.99"));
        product2.setStockQuantity(200);
        product2.setCategory("Accessories");
        product2.setBrand("TechBrand");
        product2.setImageUrls(Arrays.asList("mouse1.jpg"));
        product2 = productService.createProduct(product2);

        Price specialPrice = new Price();
        specialPrice.setProduct(product1);
        specialPrice.setPrice(new BigDecimal("899.99"));
        specialPrice.setPriceType("SPECIAL");
        specialPrice.setCustomerGroup("VIP");
        specialPrice.setMinQuantity(1);
        specialPrice.setValidFrom(LocalDateTime.now());
        specialPrice.setValidTo(LocalDateTime.now().plusDays(30));
        priceService.createPrice(specialPrice);

        Promotion promotion = new Promotion();
        promotion.setName("10% Off Electronics");
        promotion.setDescription("Get 10% off all electronics");
        promotion.setCode("ELEC10");
        promotion.setDiscountType(Promotion.DiscountType.PERCENTAGE);
        promotion.setDiscountValue(new BigDecimal("10"));
        promotion.setMinOrderAmount(new BigDecimal("50"));
        promotion.setMaxDiscountAmount(new BigDecimal("100"));
        promotion.setValidFrom(LocalDateTime.now());
        promotion.setValidTo(LocalDateTime.now().plusDays(30));
        promotion.setApplicableCategories(Arrays.asList("Electronics"));
        promotionService.createPromotion(promotion);

        logger.info("Sample data loaded for tenant: {}", tenantService.getCurrentTenant());
    }
}