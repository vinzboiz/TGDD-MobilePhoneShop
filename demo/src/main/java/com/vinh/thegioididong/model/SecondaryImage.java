package com.hutech.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SecondaryImage entity - holds additional pictures for a product.
 * Each image belongs to a product and is stored as a BLOB.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "product_images")
public class SecondaryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_data", columnDefinition = "LONGBLOB")
    private byte[] imageData;

    @Column(name = "image_type", length = 50)
    private String imageType;

    @Column(name = "image_name", length = 255)
    private String imageName;

    // order or slot index (1..3) to keep track of position
    @Column(name = "position")
    private Integer position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

}