package pos.controller;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import pos.dto.InvoicePrint;
import pos.service.InvoicePrintService;

@Controller
public class InvoicePrintController {
    private final InvoicePrintService invoices;

    public InvoicePrintController(InvoicePrintService invoices) {
        this.invoices = invoices;
    }

    @GetMapping("/invoices/{id}/print")
    public String preview(Principal actor, @PathVariable long id,
                          @RequestParam(defaultValue = "THERMAL") InvoicePrint.Paper paper, Model model) {
        model.addAttribute("invoice", invoices.preview(actor.getName(), id));
        return "pos/invoice-print";
    }
}
