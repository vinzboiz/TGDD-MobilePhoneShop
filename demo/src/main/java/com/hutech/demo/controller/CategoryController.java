package com.hutech.demo.controller;

import com.hutech.demo.model.Category;
import com.hutech.demo.model.ParentCategory;
import com.hutech.demo.service.CategoryAttributeService;
import com.hutech.demo.service.CategoryService;
import com.hutech.demo.service.ParentCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

/**
 * Category Controller - Handles category CRUD operations
 */
@Controller
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final ParentCategoryService parentCategoryService;
    private final CategoryAttributeService categoryAttributeService;

    @GetMapping
    public String listCategories(Model model) {
        model.addAttribute("parentCategories", parentCategoryService.findAll());
        model.addAttribute("childCategories", categoryService.findAllChildren());
        return "categories/categories-list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("category", new Category());
        model.addAttribute("parentCategories", parentCategoryService.findAll());
        model.addAttribute("allAttributes", categoryAttributeService.findAllOrderByParentAndSort());
        return "categories/add-category";
    }

    @PostMapping("/add")
    public String addCategory(@Valid @ModelAttribute("category") Category category,
                              BindingResult result,
                              @RequestParam(required = false) Long parentCategoryId,
                              @RequestParam(required = false) Long attributeId,
                              @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        if (parentCategoryId == null) {
            result.rejectValue("parent", "required", "Vui lòng chọn danh mục cha.");
        }
        if (result.hasErrors()) {
            model.addAttribute("parentCategories", parentCategoryService.findAll());
            model.addAttribute("allAttributes", categoryAttributeService.findAllOrderByParentAndSort());
            return "categories/add-category";
        }
        ParentCategory parentCategory = parentCategoryService.getById(parentCategoryId);
        category.setParentCategory(parentCategory);
        if (attributeId != null && attributeId > 0) {
            category.setAttribute(categoryAttributeService.getById(attributeId));
        } else {
            category.setAttribute(null);
        }
        try {
            categoryService.handleCategoryImage(category, imageFile);
        } catch (IOException e) {
            result.rejectValue("name", "error.image", "Lỗi tải ảnh: " + e.getMessage());
            model.addAttribute("parentCategories", parentCategoryService.findAll());
            model.addAttribute("allAttributes", categoryAttributeService.findAllOrderByParentAndSort());
            return "categories/add-category";
        }
        categoryService.save(category);
        redirectAttributes.addFlashAttribute("successMessage", "Đã thêm danh mục thành công!");
        return "redirect:/categories";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Category category = categoryService.getById(id);
        model.addAttribute("category", category);
        model.addAttribute("parentCategories", parentCategoryService.findAll());
        model.addAttribute("allAttributes", categoryAttributeService.findAllOrderByParentAndSort());
        return "categories/update-category";
    }

    @PostMapping("/edit/{id}")
    public String updateCategory(@PathVariable Long id,
                                 @Valid @ModelAttribute("category") Category category,
                                 BindingResult result,
                                 @RequestParam(required = false) Long parentCategoryId,
                                 @RequestParam(required = false) Long attributeId,
                                 @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (parentCategoryId == null) {
            result.rejectValue("parent", "required", "Vui lòng chọn danh mục cha.");
        }
        if (result.hasErrors()) {
            category.setId(id);
            model.addAttribute("category", category);
            model.addAttribute("parentCategories", parentCategoryService.findAll());
            model.addAttribute("allAttributes", categoryAttributeService.findAllOrderByParentAndSort());
            return "categories/update-category";
        }
        Category existing = categoryService.getById(id);
        category.setId(id);
        category.setParentCategory(parentCategoryService.getById(parentCategoryId));
        category.setAttribute(attributeId != null && attributeId > 0 ? categoryAttributeService.getById(attributeId) : null);
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                categoryService.handleCategoryImage(category, imageFile);
            } catch (IOException e) {
                result.rejectValue("name", "error.image", "Lỗi tải ảnh: " + e.getMessage());
                model.addAttribute("category", category);
                model.addAttribute("parentCategories", parentCategoryService.findAll());
                model.addAttribute("allAttributes", categoryAttributeService.findAllOrderByParentAndSort());
                return "categories/update-category";
            }
        } else {
            category.setImageData(existing.getImageData());
            category.setImageType(existing.getImageType());
            category.setImageName(existing.getImageName());
        }
        categoryService.save(category);
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật danh mục thành công!");
        return "redirect:/categories";
    }

    /**
     * Delete category - không cho xóa danh mục cha khi còn con.
     */
    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa danh mục thành công!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa: " + e.getMessage());
        }
        return "redirect:/categories";
    }

    /**
     * Hiển thị ảnh danh mục (dùng trong list và form).
     */
    @GetMapping("/image/{id}")
    public ResponseEntity<byte[]> getCategoryImage(@PathVariable Long id) {
        Category category = categoryService.getById(id);
        byte[] data = category.getImageData();
        if (data == null || data.length == 0) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = MediaType.IMAGE_JPEG;
        if (category.getImageType() != null && !category.getImageType().isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(category.getImageType());
            } catch (Exception ignored) {
                // fallback to JPEG
            }
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(data.length))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (category.getImageName() != null ? category.getImageName() : "image") + "\"")
                .contentType(mediaType)
                .body(data);
    }

}
