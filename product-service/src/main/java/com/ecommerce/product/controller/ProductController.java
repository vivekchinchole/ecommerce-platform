package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ProductDto;
import com.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Product Management")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Create product (ADMIN only)")
    public ResponseEntity<ProductDto.ProductResponse> createProduct(
            @Valid @RequestBody ProductDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ProductDto.ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping
    @Operation(summary = "Get all products with pagination")
    public ResponseEntity<Page<ProductDto.ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {
        return ResponseEntity.ok(productService.getAllProducts(
                PageRequest.of(page, size, Sort.by(sortBy).descending())));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product (ADMIN only)")
    public ResponseEntity<ProductDto.ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductDto.CreateRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "Update product stock")
    public ResponseEntity<ProductDto.ProductResponse> updateStock(
            @PathVariable Long id,
            @Valid @RequestBody ProductDto.StockUpdateRequest request) {
        return ResponseEntity.ok(productService.updateStock(id, request));
    }

    @PatchMapping("/{id}/stock/decrease")
    @Operation(summary = "Decrease product stock (internal use)")
    public ResponseEntity<Void> decreaseStock(
            @PathVariable Long id,
            @RequestParam int quantity) {
        productService.decreaseStock(id, quantity);
        return ResponseEntity.noContent().build();
    }
}
