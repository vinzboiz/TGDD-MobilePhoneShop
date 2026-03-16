package com.hutech.demo.repository;

import com.hutech.demo.model.CategoryAttribute;
import com.hutech.demo.model.ParentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryAttributeRepository extends JpaRepository<CategoryAttribute, Long> {

    List<CategoryAttribute> findByParentCategoryOrderBySortOrderAscIdAsc(ParentCategory parentCategory);

    List<CategoryAttribute> findByParentCategory_IdOrderBySortOrderAscIdAsc(Long parentCategoryId);

    @Query("SELECT a FROM CategoryAttribute a LEFT JOIN FETCH a.parentCategory ORDER BY a.parentCategory.sortOrder, a.parentCategory.id, a.sortOrder, a.id")
    List<CategoryAttribute> findAllWithParentOrderByParentAndSort();
}
