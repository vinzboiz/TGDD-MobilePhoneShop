package com.hutech.demo.repository;

import com.hutech.demo.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Product Repository - JPA data access layer for Product entity
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategory_Id(Long categoryId);

    /** Sản phẩm thuộc bất kỳ danh mục con nào của danh mục cha. */
    List<Product> findByCategory_ParentCategory_Id(Long parentCategoryId);

    /** Sản phẩm gán trực tiếp vào danh mục cha (không chọn danh mục con). */
    List<Product> findByParentCategory_Id(Long parentCategoryId);

    /** Sản phẩm thuộc danh mục cha: gán trực tiếp vào cha HOẶC vào danh mục con của cha. */
    @Query("SELECT p FROM Product p WHERE p.parentCategory.id = :id OR (p.category IS NOT NULL AND p.category.parentCategory.id = :id)")
    List<Product> findAllByParentCategoryId(@Param("id") Long parentCategoryId);

    List<Product> findByNameContainingIgnoreCase(String name);

    /** Lấy tất cả và fetch category (parentCategory lazy, tránh multiple JOIN FETCH gây thiếu bản ghi). */
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category")
    List<Product> findAllWithCategoryAndParent();

}
