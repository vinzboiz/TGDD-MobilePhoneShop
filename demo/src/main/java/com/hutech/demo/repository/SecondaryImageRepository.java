package com.hutech.demo.repository;

import com.hutech.demo.model.SecondaryImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecondaryImageRepository extends JpaRepository<SecondaryImage, Long> {
    List<SecondaryImage> findByProduct_IdOrderByPosition(Long productId);
}