package com.hutech.demo.controller;

import com.hutech.demo.model.Category;
import com.hutech.demo.model.ParentCategory;
import com.hutech.demo.model.Product;
import com.hutech.demo.service.CategoryAttributeService;
import com.hutech.demo.service.CategoryService;
import com.hutech.demo.service.ParentCategoryService;
import com.hutech.demo.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hutech.demo.model.SecondaryImage;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Product Controller - Handles product CRUD operations and image management
 */
@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ParentCategoryService parentCategoryService;
    private final CategoryAttributeService categoryAttributeService;
    private final com.hutech.demo.repository.SecondaryImageRepository secondaryImageRepository;

    @GetMapping
    public String listProducts(@RequestParam(required = false) Long categoryId,
                               @RequestParam(required = false) Long parentCategoryId,
                               Model model) {
        if (categoryId != null && categoryId > 0) {
            model.addAttribute("products", productService.findByCategory(categoryId));
        } else if (parentCategoryId != null && parentCategoryId > 0) {
            model.addAttribute("products", productService.findByParentCategoryId(parentCategoryId));
        } else {
            model.addAttribute("products", productService.findAll());
        }
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("parentCategories", parentCategoryService.findAll());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedParentCategoryId", parentCategoryId);
        return "products/products-list";
    }

    /**
     * Detail view for a single product
     */
    @GetMapping("/{id}")
    public String showProductDetail(@PathVariable Long id, Model model) {
        Product product = productService.getById(id);
        java.util.List<SecondaryImage> extras = secondaryImageRepository.findByProduct_IdOrderByPosition(id);
        model.addAttribute("product", product);
        model.addAttribute("extras", extras);
        return "products/product-detail";
    }

    /**
     * Build parent categories data (id, name, attributes, categories) as JSON for the add/edit form.
     */
    private void addParentCategoriesDataToModel(Model model) {
        List<ParentCategory> parents = parentCategoryService.findAll();
        List<Map<String, Object>> data = new ArrayList<>();
        for (ParentCategory p : parents) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("attributes", categoryAttributeService.findByParentCategoryId(p.getId()).stream()
                    .map(a -> Map.<String, Object>of("id", a.getId(), "name", a.getName()))
                    .collect(Collectors.toList()));
            m.put("categories", categoryService.findChildrenWithAttributeIdByParentCategoryId(p.getId()));
            data.add(m);
        }
        try {
            model.addAttribute("parentCategoriesJson", OBJECT_MAPPER.writeValueAsString(data));
        } catch (JsonProcessingException e) {
            model.addAttribute("parentCategoriesJson", "[]");
        }
        model.addAttribute("parentCategories", parents);
    }

    /**
     * Show add product form
     */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.findAll());
        addParentCategoriesDataToModel(model);
        return "products/add-product";
    }

    /**
     * Create new product with image upload
     */
    @PostMapping("/add")
    public String addProduct(@Valid @ModelAttribute("product") Product product,
                             BindingResult result,
                             @RequestParam("imageFile") MultipartFile imageFile,
                             @RequestParam(value = "imageFile2", required = false) MultipartFile imageFile2,
                             @RequestParam(value = "imageFile3", required = false) MultipartFile imageFile3,
                             @RequestParam(value = "imageFile4", required = false) MultipartFile imageFile4,
                             @RequestParam(value = "parentCategoryId", required = false) Long parentCategoryId,
                             @RequestParam(value = "categoryId", required = false) Long categoryId,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        // Bind category or parentCategory from request params
        if (categoryId != null && categoryId > 0) {
            product.setCategory(categoryService.getById(categoryId));
            product.setParentCategory(null);
        } else if (parentCategoryId != null && parentCategoryId > 0) {
            product.setParentCategory(parentCategoryService.getById(parentCategoryId));
            product.setCategory(null);
        }
        if (product.getCategory() == null && product.getParentCategory() == null) {
            result.rejectValue("category", "error.product", "Vui lòng chọn danh mục cha hoặc danh mục con.");
        }
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            addParentCategoriesDataToModel(model);
            return "products/add-product";
        }

        try {
            // Khởi tạo số lượng khuyến mãi còn lại bằng quota nếu chưa set
            if (product.getPromoQuota() != null && product.getPromoQuota() > 0 && product.getPromoRemaining() == null) {
                product.setPromoRemaining(product.getPromoQuota());
            }
            if (product.getSecondaryImages() == null) {
                product.setSecondaryImages(new java.util.ArrayList<>());
            }
            productService.handleImageUpload(product, imageFile, imageFile2, imageFile3, imageFile4);
            productService.save(product);
            redirectAttributes.addFlashAttribute("successMessage", "Product added successfully!");
            return "redirect:/products";
        } catch (IOException e) {
            result.rejectValue("imageData", "error.product", "Image upload failed: " + e.getMessage());
            model.addAttribute("categories", categoryService.findAll());
            addParentCategoriesDataToModel(model);
            return "products/add-product";
        }
    }

    /**
     * Show edit product form
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Product product = productService.getById(id);
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryService.findAll());
        addParentCategoriesDataToModel(model);
        Map<String, Long> initial = productService.getCategoryFormInitialIds(id);
        model.addAttribute("initialParentCategoryId", initial.get("parentCategoryId"));
        model.addAttribute("initialCategoryId", initial.get("categoryId"));
        model.addAttribute("initialAttributeId", initial.get("attributeId"));
        return "products/update-product";
    }

    /**
     * Update product with image handling
     */
    @PostMapping("/edit/{id}")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute("product") Product product,
                                BindingResult result,
                                @RequestParam("imageFile") MultipartFile imageFile,
                                @RequestParam(value = "imageFile2", required = false) MultipartFile imageFile2,
                                @RequestParam(value = "imageFile3", required = false) MultipartFile imageFile3,
                                @RequestParam(value = "imageFile4", required = false) MultipartFile imageFile4,
                                @RequestParam(value = "parentCategoryId", required = false) Long parentCategoryId,
                                @RequestParam(value = "categoryId", required = false) Long categoryId,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (categoryId != null && categoryId > 0) {
            product.setCategory(categoryService.getById(categoryId));
            product.setParentCategory(null);
        } else if (parentCategoryId != null && parentCategoryId > 0) {
            product.setParentCategory(parentCategoryService.getById(parentCategoryId));
            product.setCategory(null);
        }
        if (product.getCategory() == null && product.getParentCategory() == null) {
            result.rejectValue("category", "error.product", "Vui lòng chọn danh mục cha hoặc danh mục con.");
        }
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            addParentCategoriesDataToModel(model);
            return "products/update-product";
        }

        try {
            Product existingProduct = productService.getById(id);

            existingProduct.setName(product.getName());
            existingProduct.setPrice(product.getPrice());
            existingProduct.setDescription(product.getDescription());
            // cập nhật số lượng tồn kho và % giảm giá
            existingProduct.setStock(product.getStock());
            existingProduct.setDiscountPercent(product.getDiscountPercent());
            existingProduct.setCategory(product.getCategory());
            existingProduct.setParentCategory(product.getParentCategory());
            existingProduct.setPromoQuota(product.getPromoQuota());
            // Nếu promoRemaining chưa có, gán bằng quota mới
            if (existingProduct.getPromoQuota() != null && existingProduct.getPromoQuota() > 0
                    && existingProduct.getPromoRemaining() == null) {
                existingProduct.setPromoRemaining(existingProduct.getPromoQuota());
            }

            // Handle image(s) if new ones uploaded
            if ((imageFile != null && !imageFile.isEmpty()) ||
                (imageFile2 != null && !imageFile2.isEmpty()) ||
                (imageFile3 != null && !imageFile3.isEmpty()) ||
                (imageFile4 != null && !imageFile4.isEmpty())) {
                // ensure secondary list exists
                if (existingProduct.getSecondaryImages() == null) {
                    existingProduct.setSecondaryImages(new java.util.ArrayList<>());
                }
                productService.handleImageUpload(existingProduct, imageFile, imageFile2, imageFile3, imageFile4);
            }

            productService.save(existingProduct);
            redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully!");
            return "redirect:/products";
        } catch (IOException e) {
            result.rejectValue("imageData", "error.product", "Image upload failed: " + e.getMessage());
            model.addAttribute("categories", categoryService.findAll());
            addParentCategoriesDataToModel(model);
            return "products/update-product";
        }
    }

    /**
     * Delete product
     */
    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Product deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting product: " + e.getMessage());
        }
        return "redirect:/products";
    }

    /**
     * Display product image
     */
    @GetMapping("/image/{id}")
    public ResponseEntity<byte[]> getProductImage(@PathVariable Long id) {
        return getProductImage(id, 1);
    }

    /**
     * Retrieve a specific image index (1=primary, 2-4 secondary)
     */
    @GetMapping("/image/{id}/{index}")
    public ResponseEntity<byte[]> getProductImage(@PathVariable Long id,
                                                  @PathVariable int index) {
        try {
            Product product = productService.getById(id);
            byte[] data = null;
            String type = null;
            String name = null;
            if (index == 1) {
                data = product.getImageData();
                type = product.getImageType();
                name = product.getImageName();
            } else {
                if (product.getSecondaryImages() != null) {
                    for (SecondaryImage si : product.getSecondaryImages()) {
                        if (si.getPosition() == index - 1) {
                            data = si.getImageData();
                            type = si.getImageType();
                            name = si.getImageName();
                            break;
                        }
                    }
                }
            }
            if (data == null || data.length == 0) {
                return ResponseEntity.notFound().build();
            }
            MediaType mediaType = MediaType.IMAGE_JPEG;
            if (type != null && !type.isBlank()) {
                try {
                    mediaType = MediaType.parseMediaType(type);
                } catch (Exception ignored) {
                    // fallback to JPEG
                }
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(data.length))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (name != null ? name : "image") + "\"")
                    .contentType(mediaType)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
