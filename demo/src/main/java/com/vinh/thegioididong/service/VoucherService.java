package com.hutech.demo.service;

import com.hutech.demo.model.User;
import com.hutech.demo.model.UserVoucher;
import com.hutech.demo.repository.UserRepository;
import com.hutech.demo.repository.UserVoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Dịch vụ đổi điểm tích lũy sang thẻ khuyến mãi.
 * 100 điểm = 10k VND, 200 điểm = 20k VND, 300 điểm = 30k VND, 500 điểm = 50k VND.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class VoucherService {

    public static final int[] VOUCHER_VALUES = {10_000, 20_000, 30_000, 50_000};

    /** Số điểm cần để đổi 1 VND: 100 điểm = 10.000₫ → 1₫ cần 0,01 điểm → points = vnd/100 */
    public static int pointsRequiredForVoucher(int voucherValueVnd) {
        return voucherValueVnd / 100;
    }

    private final UserVoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final TotpService totpService;

    /**
     * Danh sách voucher còn dùng được của user.
     */
    public List<UserVoucher> getAvailableVouchers(User user) {
        return voucherRepository.findByUserAndUsedOrderByCreatedAtDesc(user, false);
    }

    /**
     * Đổi điểm sang thẻ khuyến mãi. Yêu cầu xác thực TOTP (Microsoft/Google Authenticator).
     * @param totpCode Mã 6 số từ ứng dụng Authenticator
     * @return UserVoucher mới tạo
     */
    public UserVoucher redeem(User user, int voucherValue, int totpCode) {
        if (!isValidVoucherValue(voucherValue)) {
            throw new RuntimeException("Giá trị thẻ không hợp lệ. Chọn 10k, 20k, 30k hoặc 50k.");
        }
        String secret = user.getTotpSecret();
        if (secret == null || secret.isBlank()) {
            throw new RuntimeException("Bạn chưa thiết lập Authenticator. Vui lòng thiết lập trước khi đổi điểm.");
        }
        if (!totpService.verify(secret, totpCode)) {
            throw new RuntimeException("Mã xác thực không đúng. Vui lòng thử lại.");
        }

        int points = pointsRequiredForVoucher(voucherValue); // 100 điểm = 10k VND
        Integer current = user.getTotalRewardPoints();
        if (current == null) current = 0;
        if (current < points) {
            throw new RuntimeException("Điểm tích lũy không đủ. Bạn có " + current + " điểm, cần " + points + " điểm.");
        }

        String code = generateUniqueCode();
        UserVoucher voucher = new UserVoucher();
        voucher.setUser(user);
        voucher.setVoucherValue(voucherValue);
        voucher.setCode(code);
        voucher.setUsed(false);
        voucherRepository.save(voucher);

        user.setTotalRewardPoints(current - points);
        userRepository.save(user);

        return voucher;
    }

    /**
     * Áp dụng voucher vào đơn hàng. Trả về giá trị giảm (VND), 0 nếu không hợp lệ.
     */
    public int applyVoucher(User user, String voucherCode, int orderSubtotalVnd) {
        if (voucherCode == null || voucherCode.isBlank()) return 0;

        UserVoucher v = voucherRepository.findByCodeAndUserAndUsedFalse(voucherCode.trim(), user)
                .orElse(null);
        if (v == null) return 0;

        int value = v.getVoucherValue();
        return Math.min(value, orderSubtotalVnd);
    }

    /**
     * Đánh dấu voucher đã dùng cho đơn hàng.
     */
    public void markVoucherUsed(String voucherCode, User user, Long orderId) {
        if (voucherCode == null || voucherCode.isBlank()) return;

        voucherRepository.findByCodeAndUserAndUsedFalse(voucherCode.trim(), user)
                .ifPresent(v -> {
                    v.setUsed(true);
                    v.setUsedOrderId(orderId);
                    voucherRepository.save(v);
                });
    }

    private boolean isValidVoucherValue(int value) {
        for (int v : VOUCHER_VALUES) {
            if (v == value) return true;
        }
        return false;
    }

    private String generateUniqueCode() {
        String code;
        int attempts = 0;
        do {
            code = "VOUCHER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase().replace("-", "");
            attempts++;
        } while (voucherRepository.existsByCode(code) && attempts < 10);
        if (voucherRepository.existsByCode(code)) {
            code = "VOUCHER-" + System.currentTimeMillis();
        }
        return code;
    }
}
