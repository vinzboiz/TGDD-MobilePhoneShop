package com.hutech.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Đặc tính của danh mục cha (vd: Phụ kiện di động, Phụ kiện laptop PC, Thiết bị âm thanh, ...).
 * Dùng để nhóm danh mục con trong mega-menu.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"parentCategory", "categories"})
@Entity
@Table(name = "category_attributes")
public class CategoryAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id", nullable = false)
    private ParentCategory parentCategory;

    @NotBlank(message = "Tên đặc tính không được trống")
    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /** Danh mục con thuộc đặc tính này (thứ tự: sortOrder, id = mục thêm trước hiển thị trước). */
    @OneToMany(mappedBy = "attribute", fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, id ASC")
    private List<Category> categories = new ArrayList<>();
}
