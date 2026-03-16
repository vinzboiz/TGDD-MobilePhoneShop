package com.hutech.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * Category Entity - Danh mục phân cấp: danh mục cha (parent=null) cố định, danh mục con (parent!=null) do user thêm.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"products", "children", "parent", "parentCategory", "attribute"})
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Category name cannot be blank")
    @Column(nullable = false, length = 255)
    private String name;

    /** Danh mục cha (null = danh mục cha cố định). Khi thêm mới bắt buộc chọn parent. @deprecated Ưu tiên dùng parentCategory. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    /** Danh mục con (chỉ có khi đây là danh mục cha). @deprecated Dùng ParentCategory.categories. */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Category> children;

    /** Danh mục cha (bảng riêng) - dùng cho menu và phân nhóm. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private ParentCategory parentCategory;

    /** Đặc tính (vd: Phụ kiện di động, Phụ kiện laptop) - tùy chọn, dùng để nhóm trong mega-menu. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_attribute_id")
    private CategoryAttribute attribute;

    /** Thứ tự sắp xếp (nhỏ hiển thị trước). */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /** Ảnh danh mục (tùy chọn). */
    @Column(name = "image_data", columnDefinition = "LONGBLOB")
    private byte[] imageData;
    @Column(name = "image_type", length = 128)
    private String imageType;
    @Column(name = "image_name", length = 255)
    private String imageName;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Product> products;

    /** True nếu là danh mục cha (parent == null). */
    public boolean isParent() {
        return parent == null;
    }
}
