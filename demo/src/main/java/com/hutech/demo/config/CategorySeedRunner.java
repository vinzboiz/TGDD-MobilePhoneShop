package com.hutech.demo.config;

import com.hutech.demo.service.CategoryService;
import com.hutech.demo.service.ParentCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seed danh mục cha cố định; sau đó migrate sang bảng parent_categories nếu cần.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class CategorySeedRunner implements ApplicationRunner {

    private final CategoryService categoryService;
    private final ParentCategoryService parentCategoryService;

    @Override
    public void run(ApplicationArguments args) {
        categoryService.ensureParentCategoriesSeeded();
        parentCategoryService.migrateFromCategoriesIfNeeded();
    }
}
