package com.hutech.demo.controller;

import com.hutech.demo.service.CategoryService;
import com.hutech.demo.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Home Controller - Handles home page routing
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CategoryService categoryService;
    private final ProductService productService;

    /**
     * Home page - hiển thị toàn bộ sản phẩm (flash sale)
     */
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("showHeaderTopBar", true);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("products", productService.findAll());
        return "home/home";
    }

}
