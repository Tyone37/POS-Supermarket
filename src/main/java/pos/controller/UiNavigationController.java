package pos.controller;

import java.security.Principal;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pos.service.SaleService;
import pos.service.ShiftService;

/** Opens an existing draft without creating records on a GET request. */
@Controller
public class UiNavigationController {
    private final ShiftService shifts;
    private final SaleService sales;
    public UiNavigationController(ShiftService shifts, SaleService sales) { this.shifts = shifts; this.sales = sales; }

    @GetMapping("/sales/workspace")
    public String workspace(Principal actor, Model model) {
        var current = shifts.current(actor.getName());
        if (current.isEmpty() || !"OPEN".equals(current.get().get("status"))) return "redirect:/shifts";
        var draft = sales.list(actor.getName()).stream().filter(order -> "DRAFT".equals(order.get("status"))
                && current.get().get("id").equals(order.get("shift_id"))).findFirst();
        if (draft.isPresent()) return "redirect:/sales/" + draft.get().get("id");
        model.addAttribute("username", actor.getName());
        model.addAttribute("canCashier", true); model.addAttribute("canManager", false); model.addAttribute("canAdmin", false);
        model.addAttribute("requestKey", UUID.randomUUID().toString());
        return "pos/sale-start";
    }
}
