package com.shoppingcart.multitenant.service;

import com.shoppingcart.multitenant.model.Price;
import com.shoppingcart.multitenant.model.Product;
import com.shoppingcart.multitenant.repository.PriceRepository;
import com.shoppingcart.multitenant.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PriceService {

    private static final Logger logger = LoggerFactory.getLogger(PriceService.class);

    @Autowired
    private PriceRepository priceRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TenantService tenantService;

    public BigDecimal getEffectivePrice(Long productId, String customerGroup, Integer quantity) {
        logger.debug("Getting effective price for product {} for tenant: {}", 
                    productId, tenantService.getCurrentTenant());
        
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            throw new RuntimeException("Product not found with id: " + productId);
        }

        Product product = productOpt.get();
        Optional<Price> effectivePrice = priceRepository.findEffectivePrice(
            product, customerGroup, quantity, LocalDateTime.now()
        );

        return effectivePrice.map(Price::getPrice).orElse(product.getBasePrice());
    }

    public List<Price> getProductPrices(Long productId) {
        logger.debug("Getting all prices for product {} for tenant: {}", 
                    productId, tenantService.getCurrentTenant());
        
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            throw new RuntimeException("Product not found with id: " + productId);
        }

        return priceRepository.findByProductAndActiveTrue(productOpt.get());
    }

    public List<Price> getPricesByType(String priceType) {
        logger.debug("Getting prices by type {} for tenant: {}", 
                    priceType, tenantService.getCurrentTenant());
        return priceRepository.findByPriceTypeAndActiveTrue(priceType);
    }

    public Price createPrice(Price price) {
        logger.info("Creating new price for product {} for tenant: {}", 
                   price.getProduct().getId(), tenantService.getCurrentTenant());
        return priceRepository.save(price);
    }

    public Price updatePrice(Long id, Price priceUpdates) {
        logger.info("Updating price {} for tenant: {}", id, tenantService.getCurrentTenant());
        
        Optional<Price> existingPrice = priceRepository.findById(id);
        if (existingPrice.isEmpty()) {
            throw new RuntimeException("Price not found with id: " + id);
        }

        Price price = existingPrice.get();
        if (priceUpdates.getPrice() != null) {
            price.setPrice(priceUpdates.getPrice());
        }
        if (priceUpdates.getPriceType() != null) {
            price.setPriceType(priceUpdates.getPriceType());
        }
        if (priceUpdates.getCustomerGroup() != null) {
            price.setCustomerGroup(priceUpdates.getCustomerGroup());
        }
        if (priceUpdates.getMinQuantity() != null) {
            price.setMinQuantity(priceUpdates.getMinQuantity());
        }
        if (priceUpdates.getValidFrom() != null) {
            price.setValidFrom(priceUpdates.getValidFrom());
        }
        if (priceUpdates.getValidTo() != null) {
            price.setValidTo(priceUpdates.getValidTo());
        }
        if (priceUpdates.getActive() != null) {
            price.setActive(priceUpdates.getActive());
        }

        return priceRepository.save(price);
    }

    public void deletePrice(Long id) {
        logger.info("Deleting price {} for tenant: {}", id, tenantService.getCurrentTenant());
        priceRepository.deleteById(id);
    }

    public void deactivatePrice(Long id) {
        logger.info("Deactivating price {} for tenant: {}", id, tenantService.getCurrentTenant());
        
        Optional<Price> price = priceRepository.findById(id);
        if (price.isPresent()) {
            price.get().setActive(false);
            priceRepository.save(price.get());
        } else {
            throw new RuntimeException("Price not found with id: " + id);
        }
    }
}