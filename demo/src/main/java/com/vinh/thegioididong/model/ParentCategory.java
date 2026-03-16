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
 * Danh mục cha (bảng riêng) - Điện thoại, Laptop, Phụ kiện, ...
 * Mỗi danh mục cha có nhiều "đặc tính" (CategoryAttribute) và nhiều danh mục con (Category).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"attributes", "categories"})
@Entity
@Table(name = "parent_categories")
public class ParentCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên danh mục cha không được trống")
    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /** Đặc tính của danh mục cha (vd: Phụ kiện → Phụ kiện di động, Phụ kiện laptop, ...). */
    @OneToMany(mappedBy = "parentCategory", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CategoryAttribute> attributes = new ArrayList<>();

    /** Danh mục con thuộc danh mục cha này (thứ tự: sortOrder, id). */
    @OneToMany(mappedBy = "parentCategory", fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, id ASC")
    private List<Category> categories = new ArrayList<>();
}
