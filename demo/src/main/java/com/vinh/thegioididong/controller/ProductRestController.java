package com.hutech.demo.controller;

import com.hutech.demo.model.Product;
import com.hutech.demo.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductRestController {

    private final ProductService productService;

    public record ProductDto(
            Long id,
            String name,
            Double price,
            Integer stock,
            String description,
            Long categoryId,
            String categoryName
    ) {}

    @GetMapping
    public List<ProductDto> listProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String q
    ) {
        List<Product> products;
        if (categoryId != null && categoryId > 0) {
            products = productService.findByCategory(categoryId);
        } else if (q != null && !q.isBlank()) {
            products = productService.searchByName(q);
        } else {
            products = productService.findAll();
        }
        return products.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ProductDto getProduct(@PathVariable Long id) {
        Product product = productService.getById(id);
        return toDto(product);
    }

    private ProductDto toDto(Product product) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getDescription(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null
        );
    }
}

