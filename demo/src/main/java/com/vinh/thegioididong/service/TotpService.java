package com.hutech.demo.service;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

/**
 * Dịch vụ TOTP (RFC 6238) cho Google/Microsoft Authenticator.
 */
@Service
public class TotpService {

    private static final String OTP_ISSUER = "TheGioiDiDong";
    private static final int SECRET_SIZE_BITS = 160;
    private static final Base32 BASE32 = new Base32();

    /**
     * Sinh secret Base32 mới cho TOTP.
     */
    public String generateSecret() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA1");
            keyGen.init(SECRET_SIZE_BITS);
            Key key = keyGen.generateKey();
            return BASE32.encodeToString(key.getEncoded()).replace("=", "");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate TOTP secret", e);
        }
    }

    /**
     * Tạo URL otpauth://... để sinh QR code.
     */
    public String getOtpAuthUrl(String secret, String accountName) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                OTP_ISSUER,
                accountName.replace(" ", "%20"),
                secret,
                OTP_ISSUER);
    }

    /**
     * Sinh QR code PNG dạng base64 data URL.
     */
    public String generateQrCodeDataUrl(String otpAuthUrl, int width, int height) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 2);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(otpAuthUrl, BarcodeFormat.QR_CODE, width, height, hints);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            byte[] png = out.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(png);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    /**
     * Xác thực mã 6 số từ Authenticator.
     */
    public boolean verify(String secret, int code) {
        if (secret == null || secret.isBlank()) {
            return false;
        }
        try {
            byte[] keyBytes = BASE32.decode(secret.toUpperCase());
            Key key = new SecretKeySpec(keyBytes, "HmacSHA1");
            TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator();
            int generated = totp.generateOneTimePassword(key, java.time.Instant.now());
            return generated == code;
        } catch (Exception e) {
            return false;
        }
    }
}
