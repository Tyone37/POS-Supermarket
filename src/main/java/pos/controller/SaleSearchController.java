package pos.controller;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pos.exception.FormValidationException;
import pos.service.CurrentActorService;
import pos.service.ProductSearchService;
import pos.service.SaleService;

@Controller
@RequestMapping("/sale/orders/{orderId}/products")
public class SaleSearchController {

    private final SaleService sales;
    private final ProductSearchService search;
    private final CurrentActorService actors;

    public SaleSearchController(SaleService sales, ProductSearchService search, CurrentActorService actors) {
        this.sales = sales;
        this.search = search;
        this.actors = actors;
    }

    @GetMapping
    public String search(@PathVariable Long orderId, @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Long categoryId, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("sale", sales.view(orderId));
        model.addAttribute("actorName", actors.requireEmployee().fullName());
        model.addAttribute("sectionTitle", "Tìm kiếm sản phẩm");
        model.addAttribute("query", q);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("categories", search.categories());
        try {
            model.addAttribute("products", search.search(orderId, q, categoryId, page));
        } catch (FormValidationException exception) {
            model.addAttribute("errorMessage", exception.getFieldErrors().values().iterator().next());
            model.addAttribute("products", Page.empty());
        }
        return "sale/search";
    }

    @PostMapping("/{productId}")
    public String add(@PathVariable Long orderId, @PathVariable Long productId, RedirectAttributes redirect) {
        try {
            sales.addProduct(orderId, productId);
        } catch (FormValidationException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getFieldErrors().values().iterator().next());
        }
        return "redirect:/sale/orders/" + orderId;
    }
}
