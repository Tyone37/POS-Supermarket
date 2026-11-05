package pos.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pos.dto.Forms;
import pos.exception.BusinessException;
import pos.repository.PosRepository;

@Service
public class AccountAdminService {
    private final PosRepository db;
    private final PasswordEncoder encoder;

    public AccountAdminService(PosRepository db, PasswordEncoder encoder) { this.db = db; this.encoder = encoder; }

    @Transactional
    public List<Map<String, Object>> list(String username) {
        PosSupport.actor(db, username, "ADMIN");
        return db.accounts();
    }

    public List<Map<String, Object>> roles() { return db.roles(); }

    @Transactional
    public long create(String actor, Forms.NewAccount form) {
        PosSupport.actor(db, actor, "ADMIN");
        String username = PosSupport.text(form.username(), "Tên đăng nhập", 100);
        if (!username.matches("[a-zA-Z0-9._-]{3,100}")) {
            throw new BusinessException("Tên đăng nhập gồm 3–100 chữ, số hoặc dấu . _ -.");
        }
        String name = PosSupport.text(form.fullName(), "Họ tên", 200);
        String password = form.password();
        if (password == null || password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException("Mật khẩu cần ít nhất 8 ký tự và tối đa 72 byte UTF-8.");
        }
        if (!Set.of("CASHIER", "MANAGER", "ADMIN").contains(form.roleCode() == null ? "" : form.roleCode())) {
            throw new BusinessException("Vui lòng chọn vai trò hợp lệ.");
        }
        var role = db.roles().stream().filter(row -> form.roleCode().equals(row.get("code"))).findFirst()
                .orElseThrow(() -> new BusinessException("Vai trò chưa được cấu hình."));
        if (db.account(username, false).isPresent()) {
            throw new BusinessException("Tên đăng nhập đã tồn tại.");
        }
        long id = db.createAccount(username, encoder.encode(password), name, PosSupport.id(role, "id"), PosSupport.now());
        if ("CASHIER".equals(form.roleCode())) {
            db.createEmployee(id, "NV-" + id);
        }
        return id;
    }

    @Transactional
    public void lock(String username, long target, boolean confirmed) {
        var actor = PosSupport.actor(db, username, "ADMIN");
        if (!confirmed) {
            throw new BusinessException("Vui lòng xác nhận khóa tài khoản.");
        }
        if (PosSupport.id(actor, "id") == target) {
            throw new BusinessException("Không thể tự khóa tài khoản đang sử dụng.");
        }
        db.accountById(target).orElseThrow(() -> new BusinessException("Tài khoản không tồn tại."));
        db.lockAccount(target, PosSupport.now());
    }
}
