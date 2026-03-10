package com.hutech.demo.repository;

import com.hutech.demo.model.ParentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParentCategoryRepository extends JpaRepository<ParentCategory, Long> {

    /** Chỉ fetch attributes (một bag). attribute.categories lazy-load khi cần (tránh MultipleBagFetchException). */
    @EntityGraph(attributePaths = {"attributes"})
    List<ParentCategory> findAllByOrderBySortOrderAscIdAsc();
}
