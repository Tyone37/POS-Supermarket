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
import org.springframework.web.bind.annotation.RequestParam;
import pos.dto.InvoiceSearch;
import pos.exception.BusinessException;
import pos.service.InvoiceSearchService;

@Controller
public class InvoiceSearchController {
    private final InvoiceSearchService invoices;

    public InvoiceSearchController(InvoiceSearchService invoices) {
        this.invoices = invoices;
    }

    @ModelAttribute
    void common(Model model, Authentication authentication) {
        var roles = authentication.getAuthorities().stream().map(Object::toString).toList();
        model.addAttribute("canCashier", roles.contains("ROLE_CASHIER"));
        model.addAttribute("canManager", roles.contains("ROLE_MANAGER"));
        model.addAttribute("canAdmin", roles.contains("ROLE_ADMIN"));
    }

    @GetMapping("/invoices")
    public String search(Principal actor, @Valid @ModelAttribute("filter") InvoiceSearch.Filter filter,
                         BindingResult errors, @RequestParam(defaultValue = "0") int page,
                         Model model, HttpServletResponse response) {
        if (!errors.hasErrors()) {
            try {
                model.addAttribute("result", invoices.search(actor.getName(), filter, page));
                return "pos/invoices";
            } catch (BusinessException exception) {
                model.addAttribute("error", exception.getMessage());
            }
        }
        response.setStatus(400);
        model.addAttribute("invalidFilter", true);
        model.addAttribute("result", new InvoiceSearch.Result(List.of(), 0, false));
        return "pos/invoices";
    }
}
