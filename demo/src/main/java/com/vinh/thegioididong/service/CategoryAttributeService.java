package com.hutech.demo.service;

import com.hutech.demo.model.CategoryAttribute;
import com.hutech.demo.model.ParentCategory;
import com.hutech.demo.repository.CategoryAttributeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoryAttributeService {

    private final CategoryAttributeRepository attributeRepository;

    /** Tất cả đặc tính, sắp xếp theo parent rồi sort_order (cho dropdown khi thêm/sửa danh mục). */
    public List<CategoryAttribute> findAllOrderByParentAndSort() {
        return attributeRepository.findAllWithParentOrderByParentAndSort();
    }

    public List<CategoryAttribute> findByParentCategoryId(Long parentCategoryId) {
        return attributeRepository.findByParentCategory_IdOrderBySortOrderAscIdAsc(parentCategoryId);
    }

    public List<CategoryAttribute> findByParentCategory(ParentCategory parent) {
        return attributeRepository.findByParentCategoryOrderBySortOrderAscIdAsc(parent);
    }

    public CategoryAttribute getById(Long id) {
        return attributeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CategoryAttribute not found: " + id));
    }

    public CategoryAttribute save(CategoryAttribute attr) {
        return attributeRepository.save(attr);
    }

    public void deleteById(Long id) {
        attributeRepository.deleteById(id);
    }
}
