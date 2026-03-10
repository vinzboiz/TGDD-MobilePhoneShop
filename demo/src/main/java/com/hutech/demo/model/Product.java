package com.hutech.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Product Entity - Represents a product with image storage
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name cannot be blank")
    @Column(nullable = false, length = 255)
    private String name;

    @NotNull(message = "Price cannot be null")
    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer stock = 0;  // Default to 0

    @Min(0)
    @Max(100)
    @Column(nullable = false)
    private Integer discountPercent = 0; // 0-100%

    @Column(columnDefinition = "LONGTEXT")
    private String description;

    /** Danh mục con (chi tiết). Null nếu sản phẩm gán trực tiếp vào danh mục cha. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = true)
    private Category category;

    /** Danh mục cha (khi sản phẩm chỉ gán vào cha, không chọn con). Null nếu đã chọn category con. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private ParentCategory parentCategory;

    // Image storage as BLOB
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "LONGBLOB")
    private byte[] imageData;

    @Column(length = 50)
    private String imageType; // e.g., image/png, image/jpeg

    @Column(length = 255)
    private String imageName; // Original filename

    // secondary pictures managed in a separate table
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<SecondaryImage> secondaryImages;

}
