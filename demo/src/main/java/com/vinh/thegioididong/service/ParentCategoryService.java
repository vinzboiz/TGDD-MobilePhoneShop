package com.hutech.demo.service;

import com.hutech.demo.model.Category;
import com.hutech.demo.model.ParentCategory;
import com.hutech.demo.repository.CategoryRepository;
import com.hutech.demo.repository.ParentCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class ParentCategoryService {

    private final ParentCategoryRepository parentCategoryRepository;
    private final CategoryRepository categoryRepository;

    /** Cho menu: load cha + đặc tính + danh mục con theo đặc tính. */
    public List<ParentCategory> findAllForMenu() {
        return parentCategoryRepository.findAllByOrderBySortOrderAscIdAsc();
    }

    public List<ParentCategory> findAll() {
        return parentCategoryRepository.findAllByOrderBySortOrderAscIdAsc();
    }

    public Optional<ParentCategory> findById(Long id) {
        return parentCategoryRepository.findById(id);
    }

    public ParentCategory getById(Long id) {
        return parentCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ParentCategory not found: " + id));
    }

    public ParentCategory save(ParentCategory pc) {
        return parentCategoryRepository.save(pc);
    }

    /**
     * Migration: copy danh mục cha từ bảng categories sang parent_categories và set parent_category_id cho con.
     * Chỉ chạy khi chưa có bản ghi parent_categories.
     */
    public void migrateFromCategoriesIfNeeded() {
        if (parentCategoryRepository.count() > 0) return;
        List<Category> parents = categoryRepository.findByParentIsNullOrderBySortOrderAscIdAsc();
        for (Category oldParent : parents) {
            ParentCategory newParent = new ParentCategory();
            newParent.setName(oldParent.getName());
            newParent.setSortOrder(oldParent.getSortOrder());
            newParent = parentCategoryRepository.save(newParent);
            List<Category> children = categoryRepository.findByParentOrderBySortOrderAscIdAsc(oldParent);
            for (Category child : children) {
                child.setParentCategory(newParent);
                categoryRepository.save(child);
            }
        }
    }
}
