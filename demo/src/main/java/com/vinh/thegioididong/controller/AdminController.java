package com.hutech.demo.controller;

import com.hutech.demo.model.Category;
import com.hutech.demo.service.CategoryService;
import com.hutech.demo.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping
    public String dashboard(Model model) {
        var products = productService.findAll();
        var categories = categoryService.findAllParents();

        model.addAttribute("productCount", products.size());
        model.addAttribute("categoryCount", categories.size());

        // Thống kê số sản phẩm theo danh mục con/cha
        java.util.Map<String, Long> productCountByCategory = new java.util.LinkedHashMap<>();
        for (Category c : categoryService.findAllChildren()) {
            long count = c.getProducts() != null ? c.getProducts().size() : 0L;
            if (count > 0) {
                productCountByCategory.put(c.getName(), count);
            }
        }
        model.addAttribute("productCountByCategory", productCountByCategory);
        return "admin/dashboard";
    }
}

