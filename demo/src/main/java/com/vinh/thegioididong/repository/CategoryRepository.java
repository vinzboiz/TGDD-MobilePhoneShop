package com.hutech.demo.repository;

import com.hutech.demo.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Category Repository - JPA data access layer for Category entity
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Danh mục cha (parent = null), load luôn children để mega-menu hiển thị khi hover. */
    @EntityGraph(attributePaths = {"children"})
    List<Category> findByParentIsNullOrderBySortOrderAscIdAsc();

    /** Danh mục con (có parent_category), sắp xếp theo parent rồi sortOrder. */
    List<Category> findByParentCategoryIsNotNullOrderByParentCategory_IdAscSortOrderAscIdAsc();

    /** Danh mục con (có parent), sắp xếp theo parent rồi sortOrder. */
    List<Category> findByParentIsNotNullOrderByParent_IdAscSortOrderAscIdAsc();

    /** Danh mục con theo parent, sắp xếp theo sortOrder. */
    List<Category> findByParentOrderBySortOrderAscIdAsc(Category parent);

    /** Danh mục con theo danh mục cha (parent_categories). */
    List<Category> findByParentCategory_IdOrderBySortOrderAscIdAsc(Long parentCategoryId);

    /** Danh mục con theo danh mục cha và đặc tính. */
    List<Category> findByParentCategory_IdAndAttribute_IdOrderBySortOrderAscIdAsc(Long parentCategoryId, Long attributeId);

    /** Số danh mục con của một parent. */
    long countByParent(Category parent);
}
