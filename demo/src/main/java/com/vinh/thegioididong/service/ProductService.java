package com.hutech.demo.service;

import com.hutech.demo.model.Product;
import com.hutech.demo.repository.ProductRepository;
import com.hutech.demo.repository.SecondaryImageRepository;
import com.hutech.demo.model.SecondaryImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Product Service - Business logic for Product operations including image handling
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final SecondaryImageRepository secondaryImageRepository;

    /**
     * Get all products
     */
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    /**
     * Lấy ngẫu nhiên tối đa limit sản phẩm (dùng cho trang chủ flash sale).
     * Dùng findAll() để tránh JOIN FETCH có thể gây thiếu/trùng bản ghi; trang chủ không cần category.
     */
    public List<Product> findRandomProducts(int limit) {
        List<Product> all = productRepository.findAll();
        if (all.isEmpty()) return all;
        List<Product> copy = new ArrayList<>(all);
        Collections.shuffle(copy);
        return copy.size() <= limit ? copy : copy.subList(0, limit);
    }

    /**
     * Get product by id
     */
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    /**
     * Get product by id or throw exception
     */
    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    /** Giá trị khởi tạo cho form chọn danh mục (parent/category/attribute) khi edit product. */
    public Map<String, Long> getCategoryFormInitialIds(Long productId) {
        Product p = getById(productId);
        Map<String, Long> m = new HashMap<>();
        if (p.getParentCategory() != null) {
            m.put("parentCategoryId", p.getParentCategory().getId());
            m.put("categoryId", null);
            m.put("attributeId", null);
            return m;
        }
        if (p.getCategory() != null) {
            m.put("parentCategoryId", p.getCategory().getParentCategory() != null ? p.getCategory().getParentCategory().getId() : null);
            m.put("categoryId", p.getCategory().getId());
            m.put("attributeId", p.getCategory().getAttribute() != null ? p.getCategory().getAttribute().getId() : null);
            return m;
        }
        return m;
    }

    public List<Product> findByCategory(Long categoryId) {
        return productRepository.findByCategory_Id(categoryId);
    }

    /** Sản phẩm theo danh mục cha (gán trực tiếp vào cha hoặc vào bất kỳ danh mục con). */
    public List<Product> findByParentCategoryId(Long parentCategoryId) {
        return productRepository.findAllByParentCategoryId(parentCategoryId);
    }

    /**
     * Search products by name
     */
    public List<Product> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Save or update product
     */
    public Product save(Product product) {
        return productRepository.save(product);
    }

    /**
     * Delete product by id
     */
    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }

    /**
      * Handle image upload (primary image)
     */
    public void handleImageUpload(Product product, MultipartFile imageFile) throws IOException {
        if (imageFile != null && !imageFile.isEmpty()) {
            product.setImageData(imageFile.getBytes());
            product.setImageType(imageFile.getContentType());
            product.setImageName(imageFile.getOriginalFilename());
        }
    }

    /**
     * Save or update secondary images. Index provided for ordering.
     */
    private void saveSecondaryImages(Product product,
                                     MultipartFile image2,
                                     MultipartFile image3,
                                     MultipartFile image4) throws IOException {
        // clear existing images to simplify update
        if (product.getId() != null) {
            secondaryImageRepository.findByProduct_IdOrderByPosition(product.getId())
                    .forEach(secondaryImageRepository::delete);
            product.getSecondaryImages().clear();
        }
        int pos = 1;
        for (MultipartFile mf : new MultipartFile[]{image2, image3, image4}) {
            if (mf != null && !mf.isEmpty()) {
                SecondaryImage si = new SecondaryImage();
                si.setImageData(mf.getBytes());
                si.setImageType(mf.getContentType());
                si.setImageName(mf.getOriginalFilename());
                si.setPosition(pos);
                si.setProduct(product);
                product.getSecondaryImages().add(si);
                pos++;
            }
        }
    }

    /**
     * Handle combined upload: primary plus extras.
     */
    public void handleImageUpload(Product product,
                                  MultipartFile primary,
                                  MultipartFile image2,
                                  MultipartFile image3,
                                  MultipartFile image4) throws IOException {
        handleImageUpload(product, primary);
        saveSecondaryImages(product, image2, image3, image4);
    }

    /**
     * Check if product exists
     */
    public boolean existsById(Long id) {
        return productRepository.existsById(id);
    }

}
