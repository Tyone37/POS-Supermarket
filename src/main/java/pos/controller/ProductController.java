package pos.controller;

import java.security.Principal;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pos.dto.CreateProductRequest;
import pos.exception.ProductValidationException;
import pos.service.ProductService;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @ModelAttribute("actorName")
    public String actorName(Principal principal) {
        return principal == null ? "" : principal.getName();
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("products", service.list(page));
        return "product/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("product", new CreateProductRequest(null, null, null, null, null, null));
        populateOptions(model);
        return "product/create";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("product") CreateProductRequest request, BindingResult errors,
            Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                service.create(request);
                redirect.addFlashAttribute("successMessage", "Lưu thành công");
                return "redirect:/products";
            } catch (ProductValidationException exception) {
                exception.getFieldErrors().forEach((field, message) -> {
                    if (field.isEmpty()) {
                        errors.reject("product.create", message);
                    } else {
                        errors.rejectValue(field, "product.create", message);
                    }
                });
            }
        }
        populateOptions(model);
        return "product/create";
    }

    private void populateOptions(Model model) {
        model.addAttribute("categories", service.categories());
        model.addAttribute("units", service.units());
    }
}
