package pos.controller;

import java.security.Principal;
import java.util.UUID;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pos.dto.ShiftCloseForm;
import pos.dto.ShiftClosePreview;
import pos.exception.BusinessException;
import pos.service.ShiftClosingService;

@Controller
public class ShiftClosingController {
    private static final String PENDING_CLOSE = "pendingShiftClose";
    private final ShiftClosingService closing;

    public ShiftClosingController(ShiftClosingService closing) {
        this.closing = closing;
    }

    @ModelAttribute
    void common(Model model, Authentication authentication) {
        var roles = authentication.getAuthorities().stream().map(Object::toString).toList();
        model.addAttribute("canCashier", roles.contains("ROLE_CASHIER"));
        model.addAttribute("canManager", roles.contains("ROLE_MANAGER"));
        model.addAttribute("canAdmin", roles.contains("ROLE_ADMIN"));
    }

    @GetMapping("/shifts/close")
    public String form(Principal actor, Model model, HttpSession session) {
        session.removeAttribute(PENDING_CLOSE);
        model.addAttribute("closingForm", new ShiftCloseForm());
        return entry(actor, model);
    }

    @PostMapping("/shifts/close")
    public String preview(Principal actor, @Valid @ModelAttribute("closingForm") ShiftCloseForm form,
                          BindingResult errors, Model model, HttpSession session, HttpServletResponse response) {
        session.removeAttribute(PENDING_CLOSE);
        if (errors.hasErrors()) {
            response.setStatus(400);
            return entry(actor, model);
        }
        var preview = closing.preview(actor.getName(), form.getActualCash());
        var pending = new PendingClose(UUID.randomUUID().toString(), actor.getName(), preview);
        session.setAttribute(PENDING_CLOSE, pending);
        model.addAttribute("closing", preview);
        model.addAttribute("confirmationToken", pending.token());
        return "pos/shift-close-confirm";
    }

    @PostMapping("/shifts/close/confirm")
    public String confirm(Principal actor, @RequestParam(defaultValue = "") String confirmationToken,
                          @RequestParam(defaultValue = "false") boolean confirmed, HttpSession session,
                          RedirectAttributes flash) {
        if (!confirmed || !(session.getAttribute(PENDING_CLOSE) instanceof PendingClose pending)
                || !pending.username().equals(actor.getName()) || !pending.token().equals(confirmationToken)) {
            throw new BusinessException("Vui lòng nhập tiền thực tế và xem màn hình xác nhận trước khi kết thúc ca.");
        }
        try {
            closing.close(actor.getName(), pending.preview());
        } catch (BusinessException exception) {
            session.removeAttribute(PENDING_CLOSE);
            flash.addFlashAttribute("error", exception.getMessage());
            return "redirect:/shifts/close";
        }
        session.removeAttribute(PENDING_CLOSE);
        flash.addFlashAttribute("message", "Đã kết thúc ca và lưu kết quả bàn giao.");
        return "redirect:/shifts";
    }

    private String entry(Principal actor, Model model) {
        model.addAttribute("closing", closing.preview(actor.getName(), "0"));
        return "pos/shift-close";
    }

    private record PendingClose(String token, String username, ShiftClosePreview preview) {
    }
}
