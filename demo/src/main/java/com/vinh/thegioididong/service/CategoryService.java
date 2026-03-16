package com.hutech.demo.service;

import com.hutech.demo.model.Category;
import com.hutech.demo.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Category Service - Danh mục cha cố định, thêm mới phải chọn danh mục cha.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /** Tên các danh mục cha cố định (theo thứ tự hiển thị). */
    private static final String[] PARENT_NAMES = {
        "Điện thoại", "Laptop", "Phụ kiện", "Smartwatch", "Đồng hồ", "Tablet",
        "Máy cũ, Thu cũ", "Màn hình, Máy in", "Sim, Thẻ cào", "Dịch vụ tiện ích"
    };

    /**
     * Lấy tất cả danh mục (để filter sản phẩm, menu...).
     */
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    /**
     * Chỉ lấy danh mục cha (parent = null), dùng cho dropdown chọn cha và menu.
     */
    public List<Category> findAllParents() {
        return categoryRepository.findByParentIsNullOrderBySortOrderAscIdAsc();
    }

    /**
     * Chỉ lấy danh mục con (có parent hoặc parentCategory), dùng cho bảng riêng.
     */
    public List<Category> findAllChildren() {
        if (categoryRepository.count() == 0) return List.of();
        try {
            return categoryRepository.findByParentCategoryIsNotNullOrderByParentCategory_IdAscSortOrderAscIdAsc();
        } catch (Exception e) {
            return categoryRepository.findByParentIsNotNullOrderByParent_IdAscSortOrderAscIdAsc();
        }
    }

    /**
     * Lấy danh mục con theo danh mục cha (Category parent).
     */
    public List<Category> findChildrenByParentId(Long parentId) {
        if (parentId == null) return List.of();
        Category parent = getById(parentId);
        return categoryRepository.findByParentOrderBySortOrderAscIdAsc(parent);
    }

    /**
     * Lấy danh mục con theo danh mục cha (ParentCategory). Có thể lọc theo đặc tính.
     */
    public List<Category> findChildrenByParentCategoryId(Long parentCategoryId, Long attributeId) {
        if (parentCategoryId == null) return List.of();
        if (attributeId != null && attributeId > 0) {
            return categoryRepository.findByParentCategory_IdAndAttribute_IdOrderBySortOrderAscIdAsc(parentCategoryId, attributeId);
        }
        return categoryRepository.findByParentCategory_IdOrderBySortOrderAscIdAsc(parentCategoryId);
    }

    /**
     * Trả về danh mục con dạng [{id, name, attributeId}] để dùng cho form (trong transaction, tránh lazy).
     */
    public List<Map<String, Object>> findChildrenWithAttributeIdByParentCategoryId(Long parentCategoryId) {
        List<Category> list = findChildrenByParentCategoryId(parentCategoryId, null);
        return list.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("attributeId", c.getAttribute() != null ? c.getAttribute().getId() : null);
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * Lưu ảnh danh mục (tùy chọn).
     */
    public void handleCategoryImage(Category category, MultipartFile imageFile) throws IOException {
        if (imageFile != null && !imageFile.isEmpty()) {
            category.setImageData(imageFile.getBytes());
            category.setImageType(imageFile.getContentType());
            category.setImageName(imageFile.getOriginalFilename());
        }
    }

    /**
     * Đảm bảo đã có đủ danh mục cha cố định (gọi lúc khởi động).
     */
    public void ensureParentCategoriesSeeded() {
        List<Category> existing = categoryRepository.findByParentIsNullOrderBySortOrderAscIdAsc();
        if (!existing.isEmpty()) return;
        for (int i = 0; i < PARENT_NAMES.length; i++) {
            Category c = new Category();
            c.setName(PARENT_NAMES[i]);
            c.setParent(null);
            c.setSortOrder(i + 1);
            categoryRepository.save(c);
        }
    }

    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }

    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
    }

    public Category save(Category category) {
        return categoryRepository.save(category);
    }

    public void deleteById(Long id) {
        Category cat = getById(id);
        if (cat.isParent() && categoryRepository.countByParent(cat) > 0) {
            throw new IllegalStateException("Không thể xóa danh mục cha khi còn danh mục con.");
        }
        categoryRepository.deleteById(id);
    }

    public boolean existsById(Long id) {
        return categoryRepository.existsById(id);
    }
}
