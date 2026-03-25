package com.hutech.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Thẻ khuyến mãi do user đổi từ điểm tích lũy.
 * 10k điểm = 10k VND, 20k điểm = 20k VND, v.v.
 */
@Entity
@Table(name = "user_vouchers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVoucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Giá trị thẻ (VNĐ): 10000, 20000, 30000, 50000 */
    @Column(name = "voucher_value", nullable = false)
    private Integer voucherValue;

    /** Mã voucher (VOUCHER-XXXX-XXXX) */
    @Column(unique = true, length = 20, nullable = false)
    private String code;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "used_order_id")
    private Long usedOrderId;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
