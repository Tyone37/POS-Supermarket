package pos.controller;

import java.security.Principal;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pos.dto.Forms;
import pos.exception.BusinessException;
import pos.service.CatalogService;
import pos.service.PromotionService;

@Controller
public class PromotionController {
    private final PromotionService promotions;
    private final CatalogService catalog;

    public PromotionController(PromotionService promotions, CatalogService catalog) {
        this.promotions = promotions;
        this.catalog = catalog;
    }

    @ModelAttribute
    void common(Model model, Principal principal) {
        model.addAttribute("username", principal == null ? "" : principal.getName());
        model.addAttribute("requestKey", UUID.randomUUID().toString());
        if (principal instanceof org.springframework.security.core.Authentication auth) {
            var roles = auth.getAuthorities().stream().map(Object::toString).toList();
            model.addAttribute("canCashier", roles.contains("ROLE_CASHIER"));
            model.addAttribute("canManager", roles.contains("ROLE_MANAGER"));
            model.addAttribute("canAdmin", roles.contains("ROLE_ADMIN"));
        }
    }

    @GetMapping("/promotions")
    public String list(Principal actor, Model model) {
        model.addAttribute("promotions", promotions.list(actor.getName()));
        model.addAttribute("products", catalog.search("", null, false));
        return "pos/promotions";
    }

    @PostMapping("/promotions")
    public String create(Principal actor, @ModelAttribute Forms.Promotion form, Model model,
                         jakarta.servlet.http.HttpServletResponse response, RedirectAttributes flash) {
        try {
            long id = promotions.create(actor.getName(), form);
            flash.addFlashAttribute("message", "Đã tạo chương trình khuyến mãi.");
            return "redirect:/promotions/" + id;
        } catch (BusinessException | DataIntegrityViolationException ex) {
            response.setStatus(400);
            model.addAttribute("error", ex instanceof BusinessException ? ex.getMessage()
                    : "Mã khuyến mãi đã tồn tại hoặc dữ liệu không hợp lệ.");
            model.addAttribute("form", form);
            return list(actor, model);
        }
    }

    @GetMapping("/promotions/{id}")
    public String detail(Principal actor, @PathVariable long id, Model model) {
        model.addAttribute("promo", promotions.get(actor.getName(), id));
        model.addAttribute("products", catalog.search("", null, false));
        return "pos/promotion-detail";
    }

    @PostMapping("/promotions/{id}")
    public String update(Principal actor, @PathVariable long id, @ModelAttribute Forms.Promotion form, Model model,
                         jakarta.servlet.http.HttpServletResponse response, RedirectAttributes flash) {
        try {
            promotions.update(actor.getName(), id, form);
            flash.addFlashAttribute("message", "Đã cập nhật chương trình khuyến mãi.");
            return "redirect:/promotions/" + id;
        } catch (BusinessException | DataIntegrityViolationException ex) {
            response.setStatus(400);
            model.addAttribute("error", ex instanceof BusinessException ? ex.getMessage()
                    : "Dữ liệu cập nhật không hợp lệ.");
            model.addAttribute("form", form);
            return detail(actor, id, model);
        }
    }

    @GetMapping("/promotions/{id}/cancel")
    public String cancelConfirm(Principal actor, @PathVariable long id, Model model) {
        var promo = promotions.get(actor.getName(), id);
        String status = String.valueOf(promo.get("effective_status"));
        if (!"ACTIVE".equals(status) && !"UPCOMING".equals(status)) {
            throw new BusinessException("Chương trình đã kết thúc hoặc đã hủy trước đó.");
        }
        model.addAttribute("title", "Kết thúc sớm / Hủy khuyến mãi " + promo.get("code") + "?");
        model.addAttribute("action", "/promotions/" + id + "/cancel");
        model.addAttribute("back", "/promotions/" + id);
        return "pos/confirm";
    }

    @PostMapping("/promotions/{id}/cancel")
    public String cancel(Principal actor, @PathVariable long id,
                         @RequestParam(defaultValue = "false") boolean confirmed, RedirectAttributes flash) {
        promotions.cancel(actor.getName(), id, confirmed);
        flash.addFlashAttribute("message", "Đã kết thúc sớm / hủy chương trình khuyến mãi. Hóa đơn cũ vẫn giữ nguyên.");
        return "redirect:/promotions/" + id;
    }
}
