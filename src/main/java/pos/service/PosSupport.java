package pos.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;

import pos.exception.BusinessException;
import pos.repository.PosRepository;

final class PosSupport {
    private PosSupport() { }

    static Map<String, Object> actor(PosRepository db, String username, String role) {
        var account = db.account(username, true).orElseThrow(() -> new AccessDeniedException("Không có quyền truy cập."));
        if (!"ACTIVE".equals(account.get("status")) || (role != null && !role.equals(account.get("role_code")))) {
            throw new AccessDeniedException("Không có quyền sử dụng chức năng này.");
        }
        return account;
    }

    public static long id(Map<String, Object> row, String field) { return ((Number) row.get(field)).longValue(); }
    static Timestamp now() { return Timestamp.from(Instant.now()); }
    static String number(String prefix) { return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 24); }

    static String text(String value, String label, int max) {
        String text = value == null ? "" : value.strip();
        if (text.isBlank() || text.length() > max) {
            throw new BusinessException(label + " bắt buộc và không vượt quá " + max + " ký tự.");
        }
        return text;
    }

    static String requestKey(String value) {
        String key = text(value, "Mã yêu cầu", 64);
        if (!key.matches("[a-zA-Z0-9_-]{8,64}")) {
            throw new BusinessException("Mã yêu cầu không hợp lệ. Vui lòng tải lại trang.");
        }
        return key;
    }

    static BigDecimal money(String value, String label) {
        try {
            BigDecimal amount = new BigDecimal(value == null ? "" : value.strip());
            if (amount.signum() < 0 || amount.compareTo(new BigDecimal("9999999999999999")) > 0
                    || amount.stripTrailingZeros().scale() > 0) {
                throw new NumberFormatException();
            }
            return amount.setScale(2);
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new BusinessException(label + " phải là số nguyên đồng không âm và nằm trong giới hạn dữ liệu.");
        }
    }

    static BigDecimal quantity(String value, int scale, String label) {
        try {
            BigDecimal amount = new BigDecimal(value == null ? "" : value.strip());
            if (amount.signum() < 0 || amount.compareTo(new BigDecimal("999999999999.999")) > 0
                    || amount.stripTrailingZeros().scale() > scale) {
                throw new NumberFormatException();
            }
            return amount;
        } catch (NumberFormatException ex) {
            throw new BusinessException(label + " không hợp lệ hoặc vượt độ chính xác của đơn vị.");
        }
    }

    static String normalized(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).strip();
    }
}
