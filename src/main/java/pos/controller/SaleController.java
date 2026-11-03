package pos.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pos.dto.AddItemRequest;
import pos.exception.FormValidationException;
import pos.service.CurrentActorService;
import pos.service.SaleService;

@Controller
@RequestMapping("/sale")
public class SaleController {

    private final SaleService sales;
    private final CurrentActorService actors;

    public SaleController(SaleService sales, CurrentActorService actors) {
        this.sales = sales;
        this.actors = actors;
    }

    @ModelAttribute("actorName")
    public String actorName() {
        return actors.requireEmployee().fullName();
    }

    @ModelAttribute("sectionTitle")
    public String sectionTitle() {
        return "Bán hàng tại quầy";
    }

    @GetMapping
    public String current(Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("sale", sales.current());
            return "sale/cart";
        } catch (FormValidationException exception) {
            return shiftRequired(exception, redirect);
        }
    }

    @PostMapping("/orders")
    public String create(RedirectAttributes redirect) {
        try {
            return "redirect:/sale/orders/" + sales.create();
        } catch (FormValidationException exception) {
            return shiftRequired(exception, redirect);
        }
    }

    @GetMapping("/orders/{id}")
    public String view(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("sale", sales.view(id));
            return "sale/cart";
        } catch (FormValidationException exception) {
            return shiftRequired(exception, redirect);
        }
    }

    @PostMapping("/orders/{id}/items")
    public String add(@PathVariable Long id, @Valid @ModelAttribute AddItemRequest request, BindingResult errors,
            Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            model.addAttribute("errorMessage", errors.getAllErrors().getFirst().getDefaultMessage());
        } else {
            try {
                sales.addCode(id, request);
                return "redirect:/sale/orders/" + id;
            } catch (FormValidationException exception) {
                model.addAttribute("errorMessage", exception.getFieldErrors().values().iterator().next());
            }
        }
        model.addAttribute("inputCode", request.code());
        return view(id, model, redirect);
    }

    private String shiftRequired(FormValidationException exception, RedirectAttributes redirect) {
        redirect.addFlashAttribute("errorMessage", exception.getFieldErrors().values().iterator().next());
        return "redirect:/shifts/current";
    }
}
