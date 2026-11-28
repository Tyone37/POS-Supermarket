package pos.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pos.config.PosTime;
import pos.dto.CouponForm;
import pos.exception.BusinessException;
import pos.service.CouponService;

@Controller
public class CouponController {
    private final CouponService coupons;
    private final PosTime time;

    public CouponController(CouponService coupons, PosTime time) { this.coupons = coupons; this.time = time; }

    @ModelAttribute
    void common(Model model, Authentication auth) {
        var roles = auth.getAuthorities().stream().map(Object::toString).toList();
        model.addAttribute("canManager", roles.contains("ROLE_MANAGER"));
        model.addAttribute("canCashier", roles.contains("ROLE_CASHIER"));
        model.addAttribute("canAdmin", roles.contains("ROLE_ADMIN"));
    }

    @GetMapping("/coupons")
    public String list(Principal actor, @RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("result", coupons.list(actor.getName(), q, page));
        model.addAttribute("q", q);
        return "pos/coupons";
    }

    @GetMapping("/coupons/new")
    public String create(Principal actor, Model model) {
        model.addAttribute("form", new CouponForm("", "", "ORDER", null, "PERCENT", "", "0", "", "", "ENABLED"));
        return form(actor, null, model);
    }

    @GetMapping("/coupons/{id}/edit")
    public String edit(Principal actor, @PathVariable long id, Model model) {
        var c = coupons.get(actor.getName(), id);
        model.addAttribute("form", new CouponForm((String) c.get("code"), (String) c.get("name"),
                (String) c.get("scope"), (Long) c.get("product_id"), (String) c.get("discount_type"),
                amount(c, "discount_value"), amount(c, "min_order_amount"), time.formatInput(c.get("starts_at")),
                time.formatInput(c.get("ends_at")), (String) c.get("lifecycle_status")));
        return form(actor, id, model);
    }

    @PostMapping("/coupons")
    public String create(Principal actor, @Valid @ModelAttribute("form") CouponForm form, BindingResult errors,
                         Model model, HttpServletResponse response, RedirectAttributes flash) {
        return save(actor, null, form, errors, model, response, flash);
    }

    @PostMapping("/coupons/{id}")
    public String update(Principal actor, @PathVariable long id, @Valid @ModelAttribute("form") CouponForm form,
                         BindingResult errors, Model model, HttpServletResponse response, RedirectAttributes flash) {
        return save(actor, id, form, errors, model, response, flash);
    }

    private String save(Principal actor, Long id, CouponForm input, BindingResult errors, Model model,
                        HttpServletResponse response, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                coupons.save(actor.getName(), id, input);
                flash.addFlashAttribute("message", "Đã lưu coupon. Hóa đơn cũ giữ nguyên thông tin.");
                return "redirect:/coupons";
            } catch (BusinessException exception) {
                model.addAttribute("error", exception.getMessage());
            } catch (DataIntegrityViolationException exception) {
                errors.rejectValue("code", "duplicate", "Mã coupon đã tồn tại hoặc dữ liệu liên kết đã thay đổi.");
            }
        }
        response.setStatus(400);
        return form(actor, id, model);
    }

    private String form(Principal actor, Long id, Model model) {
        model.addAttribute("couponId", id);
        model.addAttribute("products", coupons.products(actor.getName()));
        return "pos/coupon-form";
    }

    private String amount(Map<String, Object> values, String key) {
        return ((BigDecimal) values.get(key)).stripTrailingZeros().toPlainString();
    }
}
