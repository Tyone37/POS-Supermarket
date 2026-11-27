package pos.controller;

import java.security.Principal;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import pos.dto.ShiftHistory;
import pos.exception.BusinessException;
import pos.service.ShiftHistoryService;

@Controller
public class ShiftHistoryController {
    private final ShiftHistoryService history;

    public ShiftHistoryController(ShiftHistoryService history) {
        this.history = history;
    }

    @ModelAttribute
    void common(Model model, Authentication authentication) {
        var roles = authentication.getAuthorities().stream().map(Object::toString).toList();
        model.addAttribute("canCashier", roles.contains("ROLE_CASHIER"));
        model.addAttribute("canManager", roles.contains("ROLE_MANAGER"));
        model.addAttribute("canAdmin", roles.contains("ROLE_ADMIN"));
    }

    @GetMapping("/shifts/history")
    public String search(Principal actor, @Valid @ModelAttribute("filter") ShiftHistory.Filter filter,
                         BindingResult errors, @RequestParam(defaultValue = "0") int page,
                         Model model, HttpServletResponse response) {
        if (!errors.hasErrors()) {
            try {
                var result = history.search(actor.getName(), filter, page);
                model.addAttribute("shifts", result.shifts());
                model.addAttribute("options", result.options());
                return "pos/shift-history";
            } catch (BusinessException exception) {
                model.addAttribute("error", exception.getMessage());
            }
        }
        response.setStatus(400);
        model.addAttribute("invalidFilter", true);
        model.addAttribute("options", history.options(actor.getName()));
        model.addAttribute("shifts", new ShiftHistory.Page<>(List.of(), 0, false));
        return "pos/shift-history";
    }

    @GetMapping("/shifts/history/{id}")
    public String detail(Principal actor, @PathVariable long id, @RequestParam(defaultValue = "0") int page,
                         Model model) {
        var result = history.detail(actor.getName(), id, page);
        model.addAttribute("detail", result.detail());
        model.addAttribute("invoices", result.invoices());
        return "pos/shift-history-detail";
    }
}
