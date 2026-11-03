package pos.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import pos.service.CurrentActorService;
import pos.service.InvoiceDetailService;

@Controller
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceDetailService invoices;
    private final CurrentActorService actors;

    public InvoiceController(InvoiceDetailService invoices, CurrentActorService actors) {
        this.invoices = invoices;
        this.actors = actors;
    }

    @ModelAttribute("actorName")
    public String actorName() {
        return actors.requireEmployee().fullName();
    }

    @ModelAttribute("sectionTitle")
    public String sectionTitle() {
        return "Hóa đơn";
    }

    @GetMapping
    public String lookup() {
        return "invoice/lookup";
    }

    @GetMapping("/find")
    public String find(@RequestParam String number) {
        return "redirect:/invoices/" + invoices.findByNumber(number).id();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("invoice", invoices.getDetail(id));
        return "invoice/detail";
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ModelAndView notFound(ResponseStatusException exception) {
        if (exception.getStatusCode().value() != 404) {
            throw exception;
        }
        return new ModelAndView("invoice/lookup", Map.of("errorMessage",
                "Không tìm thấy hóa đơn trong phạm vi được phép", "sectionTitle", "Hóa đơn"), HttpStatus.NOT_FOUND);
    }
}
