package com.shoppingcart.multitenant.service;

import com.shoppingcart.multitenant.model.Product;
import com.shoppingcart.multitenant.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Product Service Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private Product updatedProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setSku("TEST-001");
        testProduct.setBasePrice(new BigDecimal("99.99"));
        testProduct.setStockQuantity(10);
        testProduct.setCategory("Electronics");
        testProduct.setBrand("TestBrand");
        testProduct.setActive(true);

        updatedProduct = new Product();
        updatedProduct.setName("Updated Product");
        updatedProduct.setDescription("Updated Description");
        updatedProduct.setBasePrice(new BigDecimal("149.99"));
        updatedProduct.setStockQuantity(20);
    }

    @Test
    @DisplayName("Should get all active products")
    void shouldGetAllActiveProducts() {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        when(productRepository.findByActiveTrue()).thenReturn(products);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        List<Product> result = productService.getAllActiveProducts();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testProduct);
        verify(productRepository).findByActiveTrue();
    }

    @Test
    @DisplayName("Should get active products with pagination")
    void shouldGetActiveProductsWithPagination() {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products);
        Pageable pageable = mock(Pageable.class);
        
        when(productRepository.findByActiveTrue(pageable)).thenReturn(productPage);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        Page<Product> result = productService.getActiveProducts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testProduct);
        verify(productRepository).findByActiveTrue(pageable);
    }

    @Test
    @DisplayName("Should get product by ID")
    void shouldGetProductById() {
        // Given
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        Optional<Product> result = productService.getProductById(productId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testProduct);
        verify(productRepository).findById(productId);
    }

    @Test
    @DisplayName("Should return empty when product not found by ID")
    void shouldReturnEmptyWhenProductNotFoundById() {
        // Given
        Long productId = 999L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        Optional<Product> result = productService.getProductById(productId);

        // Then
        assertThat(result).isEmpty();
        verify(productRepository).findById(productId);
    }

    @Test
    @DisplayName("Should get product by SKU")
    void shouldGetProductBySku() {
        // Given
        String sku = "TEST-001";
        when(productRepository.findBySku(sku)).thenReturn(Optional.of(testProduct));
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        Optional<Product> result = productService.getProductBySku(sku);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testProduct);
        verify(productRepository).findBySku(sku);
    }

    @Test
    @DisplayName("Should get products by category")
    void shouldGetProductsByCategory() {
        // Given
        String category = "Electronics";
        List<Product> products = Arrays.asList(testProduct);
        when(productRepository.findByCategory(category)).thenReturn(products);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        List<Product> result = productService.getProductsByCategory(category);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testProduct);
        verify(productRepository).findByCategory(category);
    }

    @Test
    @DisplayName("Should search products")
    void shouldSearchProducts() {
        // Given
        String searchTerm = "test";
        Pageable pageable = mock(Pageable.class);
        Page<Product> productPage = new PageImpl<>(Arrays.asList(testProduct));
        
        when(productRepository.searchProducts(searchTerm, pageable)).thenReturn(productPage);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        Page<Product> result = productService.searchProducts(searchTerm, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testProduct);
        verify(productRepository).searchProducts(searchTerm, pageable);
    }

    @Test
    @DisplayName("Should create product")
    void shouldCreateProduct() {
        // Given
        when(productRepository.save(testProduct)).thenReturn(testProduct);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        Product result = productService.createProduct(testProduct);

        // Then
        assertThat(result).isEqualTo(testProduct);
        verify(productRepository).save(testProduct);
    }

    @Test
    @DisplayName("Should update existing product")
    void shouldUpdateExistingProduct() {
        // Given
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        Product result = productService.updateProduct(productId, updatedProduct);

        // Then
        assertThat(result.getName()).isEqualTo("Updated Product");
        assertThat(result.getDescription()).isEqualTo("Updated Description");
        assertThat(result.getBasePrice()).isEqualTo(new BigDecimal("149.99"));
        assertThat(result.getStockQuantity()).isEqualTo(20);
        verify(productRepository).findById(productId);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent product")
    void shouldThrowExceptionWhenUpdatingNonExistentProduct() {
        // Given
        Long productId = 999L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When & Then
        assertThatThrownBy(() -> productService.updateProduct(productId, updatedProduct))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Product not found with id: " + productId);
        
        verify(productRepository).findById(productId);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should delete product")
    void shouldDeleteProduct() {
        // Given
        Long productId = 1L;
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        productService.deleteProduct(productId);

        // Then
        verify(productRepository).deleteById(productId);
    }

    @Test
    @DisplayName("Should deactivate existing product")
    void shouldDeactivateExistingProduct() {
        // Given
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When
        productService.deactivateProduct(productId);

        // Then
        verify(productRepository).findById(productId);
        verify(productRepository).save(argThat(product -> !product.getActive()));
    }

    @Test
    @DisplayName("Should throw exception when deactivating non-existent product")
    void shouldThrowExceptionWhenDeactivatingNonExistentProduct() {
        // Given
        Long productId = 999L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        when(tenantService.getCurrentTenant()).thenReturn("tenant1");

        // When & Then
        assertThatThrownBy(() -> productService.deactivateProduct(productId))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Product not found with id: " + productId);
        
        verify(productRepository).findById(productId);
        verify(productRepository, never()).save(any(Product.class));
    }
}