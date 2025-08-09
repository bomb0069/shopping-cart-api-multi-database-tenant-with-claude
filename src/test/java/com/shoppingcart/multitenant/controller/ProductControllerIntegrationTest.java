package com.shoppingcart.multitenant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppingcart.multitenant.model.Product;
import com.shoppingcart.multitenant.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@ActiveProfiles("test")
@DisplayName("Product Controller Integration Tests")
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    private Product testProduct;
    private List<Product> productList;

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

        productList = Arrays.asList(testProduct);
    }

    @Test
    @DisplayName("Should get products with pagination")
    void shouldGetProductsWithPagination() throws Exception {
        // Given
        when(productService.getActiveProducts(any()))
            .thenReturn(new PageImpl<>(productList, PageRequest.of(0, 10), 1));

        // When & Then
        mockMvc.perform(get("/api/products")
                .param("page", "0")
                .param("size", "10")
                .header("X-Tenant-ID", "tenant1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Test Product"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Should get all products")
    void shouldGetAllProducts() throws Exception {
        // Given
        when(productService.getAllActiveProducts()).thenReturn(productList);

        // When & Then
        mockMvc.perform(get("/api/products/all")
                .header("X-Tenant-ID", "tenant1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Product"));
    }

    @Test
    @DisplayName("Should get product by ID")
    void shouldGetProductById() throws Exception {
        // Given
        when(productService.getProductById(1L)).thenReturn(Optional.of(testProduct));

        // When & Then
        mockMvc.perform(get("/api/products/1")
                .header("X-Tenant-ID", "tenant1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Product"));
    }

    @Test
    @DisplayName("Should return 404 when product not found by ID")
    void shouldReturn404WhenProductNotFoundById() throws Exception {
        // Given
        when(productService.getProductById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/products/999")
                .header("X-Tenant-ID", "tenant1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should get product by SKU")
    void shouldGetProductBySku() throws Exception {
        // Given
        when(productService.getProductBySku("TEST-001")).thenReturn(Optional.of(testProduct));

        // When & Then
        mockMvc.perform(get("/api/products/sku/TEST-001")
                .header("X-Tenant-ID", "tenant1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("TEST-001"));
    }

    @Test
    @DisplayName("Should get products by category")
    void shouldGetProductsByCategory() throws Exception {
        // Given
        when(productService.getProductsByCategory("Electronics")).thenReturn(productList);

        // When & Then
        mockMvc.perform(get("/api/products/category/Electronics")
                .header("X-Tenant-ID", "tenant1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Electronics"));
    }

    @Test
    @DisplayName("Should search products")
    void shouldSearchProducts() throws Exception {
        // Given
        when(productService.searchProducts(eq("test"), any()))
            .thenReturn(new PageImpl<>(productList));

        // When & Then
        mockMvc.perform(get("/api/products/search")
                .param("q", "test")
                .header("X-Tenant-ID", "tenant1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Test Product"));
    }

    @Test
    @DisplayName("Should create product")
    void shouldCreateProduct() throws Exception {
        // Given
        Product newProduct = new Product();
        newProduct.setName("New Product");
        newProduct.setSku("NEW-001");
        newProduct.setBasePrice(new BigDecimal("29.99"));
        newProduct.setStockQuantity(5);

        when(productService.createProduct(any(Product.class))).thenReturn(testProduct);

        // When & Then
        mockMvc.perform(post("/api/products")
                .header("X-Tenant-ID", "tenant1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("Should update product")
    void shouldUpdateProduct() throws Exception {
        // Given
        Product updateData = new Product();
        updateData.setName("Updated Product");
        updateData.setBasePrice(new BigDecimal("149.99"));

        when(productService.updateProduct(eq(1L), any(Product.class))).thenReturn(testProduct);

        // When & Then
        mockMvc.perform(put("/api/products/1")
                .header("X-Tenant-ID", "tenant1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent product")
    void shouldReturn404WhenUpdatingNonExistentProduct() throws Exception {
        // Given
        Product updateData = new Product();
        updateData.setName("Updated Product");

        when(productService.updateProduct(eq(999L), any(Product.class)))
            .thenThrow(new RuntimeException("Product not found"));

        // When & Then
        mockMvc.perform(put("/api/products/999")
                .header("X-Tenant-ID", "tenant1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should delete product")
    void shouldDeleteProduct() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/products/1")
                .header("X-Tenant-ID", "tenant1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should deactivate product")
    void shouldDeactivateProduct() throws Exception {
        // When & Then
        mockMvc.perform(patch("/api/products/1/deactivate")
                .header("X-Tenant-ID", "tenant1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return current tenant")
    void shouldReturnCurrentTenant() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/products/tenant")
                .header("X-Tenant-ID", "tenant1"))
                .andExpect(status().isOk())
                .andExpect(content().string("tenant1"));
    }

    @Test
    @DisplayName("Should handle validation errors")
    void shouldHandleValidationErrors() throws Exception {
        // Given
        Product invalidProduct = new Product();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/api/products")
                .header("X-Tenant-ID", "tenant1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProduct)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle different tenant context")
    void shouldHandleDifferentTenantContext() throws Exception {
        // Given
        when(productService.getAllActiveProducts()).thenReturn(productList);

        // When & Then for tenant1
        mockMvc.perform(get("/api/products/all")
                .header("X-Tenant-ID", "tenant1"))
                .andExpect(status().isOk());

        // When & Then for tenant2
        mockMvc.perform(get("/api/products/all")
                .header("X-Tenant-ID", "tenant2"))
                .andExpect(status().isOk());
    }
}