package com.hutech.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Order Entity - Represents a customer order
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "orderDetails")
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Column(nullable = false)
    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(length = 255)
    private String shippingAddress;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 100)
    private String paymentMethod;

    @Column(length = 255)
    private String note;

    /**
     * Điểm thưởng quy đổi từ giá trị sản phẩm (không tính phí ship).
     * Mỗi 10.000₫ tiền hàng = 1 điểm.
     */
    @Column(name = "reward_points")
    private Integer rewardPoints;

    /** Mã thẻ khuyến mãi đã áp dụng (đổi từ điểm tích lũy). */
    @Column(name = "voucher_code", length = 50)
    private String voucherCode;

    /** Số tiền giảm từ thẻ khuyến mãi (VNĐ). */
    @Column(name = "voucher_discount")
    private Integer voucherDiscount = 0;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderDetail> orderDetails;

    @PrePersist
    protected void onCreate() {
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }
        if (status == null) {
            status = OrderStatus.PENDING;
        }
    }

}

