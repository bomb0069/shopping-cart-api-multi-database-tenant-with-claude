package com.shoppingcart.multitenant.service;

import com.shoppingcart.multitenant.model.Product;
import com.shoppingcart.multitenant.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TenantService tenantService;

    public List<Product> getAllActiveProducts() {
        logger.debug("Getting all active products for tenant: {}", tenantService.getCurrentTenant());
        return productRepository.findByActiveTrue();
    }

    public Page<Product> getActiveProducts(Pageable pageable) {
        logger.debug("Getting active products page for tenant: {}", tenantService.getCurrentTenant());
        return productRepository.findByActiveTrue(pageable);
    }

    public Optional<Product> getProductById(Long id) {
        logger.debug("Getting product by ID {} for tenant: {}", id, tenantService.getCurrentTenant());
        return productRepository.findById(id);
    }

    public Optional<Product> getProductBySku(String sku) {
        logger.debug("Getting product by SKU {} for tenant: {}", sku, tenantService.getCurrentTenant());
        return productRepository.findBySku(sku);
    }

    public List<Product> getProductsByCategory(String category) {
        logger.debug("Getting products by category {} for tenant: {}", category, tenantService.getCurrentTenant());
        return productRepository.findByCategory(category);
    }

    public List<Product> getProductsByBrand(String brand) {
        logger.debug("Getting products by brand {} for tenant: {}", brand, tenantService.getCurrentTenant());
        return productRepository.findByBrand(brand);
    }

    public Page<Product> searchProducts(String search, Pageable pageable) {
        logger.debug("Searching products with term '{}' for tenant: {}", search, tenantService.getCurrentTenant());
        return productRepository.searchProducts(search, pageable);
    }

    public List<Product> getInStockProducts() {
        logger.debug("Getting in-stock products for tenant: {}", tenantService.getCurrentTenant());
        return productRepository.findInStockProducts();
    }

    public Product createProduct(Product product) {
        logger.info("Creating new product '{}' for tenant: {}", product.getName(), tenantService.getCurrentTenant());
        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product productUpdates) {
        logger.info("Updating product {} for tenant: {}", id, tenantService.getCurrentTenant());
        
        Optional<Product> existingProduct = productRepository.findById(id);
        if (existingProduct.isEmpty()) {
            throw new RuntimeException("Product not found with id: " + id);
        }

        Product product = existingProduct.get();
        if (productUpdates.getName() != null) {
            product.setName(productUpdates.getName());
        }
        if (productUpdates.getDescription() != null) {
            product.setDescription(productUpdates.getDescription());
        }
        if (productUpdates.getBasePrice() != null) {
            product.setBasePrice(productUpdates.getBasePrice());
        }
        if (productUpdates.getStockQuantity() != null) {
            product.setStockQuantity(productUpdates.getStockQuantity());
        }
        if (productUpdates.getCategory() != null) {
            product.setCategory(productUpdates.getCategory());
        }
        if (productUpdates.getBrand() != null) {
            product.setBrand(productUpdates.getBrand());
        }
        if (productUpdates.getImageUrls() != null) {
            product.setImageUrls(productUpdates.getImageUrls());
        }
        if (productUpdates.getActive() != null) {
            product.setActive(productUpdates.getActive());
        }

        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        logger.info("Deleting product {} for tenant: {}", id, tenantService.getCurrentTenant());
        productRepository.deleteById(id);
    }

    public void deactivateProduct(Long id) {
        logger.info("Deactivating product {} for tenant: {}", id, tenantService.getCurrentTenant());
        
        Optional<Product> product = productRepository.findById(id);
        if (product.isPresent()) {
            product.get().setActive(false);
            productRepository.save(product.get());
        } else {
            throw new RuntimeException("Product not found with id: " + id);
        }
    }
}