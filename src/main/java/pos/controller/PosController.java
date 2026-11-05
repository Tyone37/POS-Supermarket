package pos.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Map;
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

import pos.config.TransferSettings;
import pos.dto.Forms;
import pos.exception.BusinessException;
import pos.service.AccountAdminService;
import pos.service.CatalogService;
import pos.service.SaleService;
import pos.service.ShiftService;

@Controller
public class PosController {
    private final CatalogService catalog;
    private final AccountAdminService accounts;
    private final ShiftService shifts;
    private final SaleService sales;
    private final TransferSettings transfer;

    public PosController(CatalogService catalog, AccountAdminService accounts, ShiftService shifts,
                         SaleService sales, TransferSettings transfer) {
        this.catalog = catalog; this.accounts = accounts; this.shifts = shifts; this.sales = sales; this.transfer = transfer;
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

    @GetMapping("/shifts")
    public String shifts(Principal actor, Model model) {
        model.addAttribute("shift", shifts.current(actor.getName()).orElse(null));
        model.addAttribute("registers", shifts.registers());
        return "pos/shifts";
    }

    @PostMapping("/shifts")
    public String open(Principal actor, @RequestParam(required = false) Long registerId,
                       @RequestParam(defaultValue = "") String openingCash,
                       @RequestParam(defaultValue = "") String openRequestKey, Model model, RedirectAttributes flash,
                       jakarta.servlet.http.HttpServletResponse response) {
        try {
            shifts.open(actor.getName(), registerId, openingCash, openRequestKey);
            flash.addFlashAttribute("message", "Ca đang mở. Có thể tiếp tục bán hàng.");
            return "redirect:/sales";
        } catch (BusinessException ex) {
            response.setStatus(400);
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("openingCash", openingCash);
            model.addAttribute("registerId", registerId);
            model.addAttribute("requestKey", openRequestKey);
            return shifts(actor, model);
        }
    }

    @GetMapping("/products")
    public String products(@RequestParam(defaultValue = "") String q,
                           @RequestParam(required = false) Long category, Model model) {
        model.addAttribute("products", catalog.search(q, category, false));
        model.addAttribute("categories", catalog.categories());
        model.addAttribute("units", catalog.units());
        model.addAttribute("q", q);
        return "pos/products";
    }

    @PostMapping("/products")
    public String product(Principal actor, @ModelAttribute Forms.Product form, Model model,
                          jakarta.servlet.http.HttpServletResponse response, RedirectAttributes flash) {
        try {
            catalog.create(actor.getName(), form);
            flash.addFlashAttribute("message", "Đã thêm sản phẩm. Thu ngân có thể quét ngay tại quầy.");
            return "redirect:/products";
        } catch (BusinessException | DataIntegrityViolationException ex) {
            response.setStatus(400);
            model.addAttribute("error", ex instanceof BusinessException ? ex.getMessage() : "Mã vạch hoặc SKU đã tồn tại.");
            model.addAttribute("form", form);
            return products("", null, model);
        }
    }

    @GetMapping("/accounts")
    public String accounts(Principal actor, Model model) {
        model.addAttribute("accounts", accounts.list(actor.getName()));
        model.addAttribute("roles", accounts.roles());
        return "pos/accounts";
    }

    @PostMapping("/accounts")
    public String account(Principal actor, @ModelAttribute Forms.NewAccount form, Model model,
                          jakarta.servlet.http.HttpServletResponse response, RedirectAttributes flash) {
        try {
            accounts.create(actor.getName(), form);
            flash.addFlashAttribute("message", "Đã tạo tài khoản và gán vai trò.");
            return "redirect:/accounts";
        } catch (BusinessException | DataIntegrityViolationException ex) {
            response.setStatus(400);
            model.addAttribute("error", ex instanceof BusinessException ? ex.getMessage() : "Tên đăng nhập đã tồn tại.");
            model.addAttribute("form", new Forms.NewAccount(form.username(), null, form.fullName(), form.roleCode()));
            return accounts(actor, model);
        }
    }

    @GetMapping("/accounts/{id}/lock")
    public String lockConfirm(Principal actor, @PathVariable long id, Model model) {
        var target = accounts.list(actor.getName()).stream().filter(row -> ((Number) row.get("id")).longValue() == id)
                .findFirst().orElseThrow(() -> new BusinessException("Tài khoản không tồn tại."));
        return confirm(model, "Khóa tài khoản " + target.get("username") + "?", "/accounts/" + id + "/lock", "/accounts", null);
    }

    @PostMapping("/accounts/{id}/lock")
    public String lock(Principal actor, @PathVariable long id, @RequestParam(defaultValue = "false") boolean confirmed) {
        accounts.lock(actor.getName(), id, confirmed);
        return "redirect:/accounts";
    }

    @GetMapping("/sales")
    public String sales(Principal actor, Model model) {
        var current = shifts.current(actor.getName());
        if (current.isEmpty() || !"OPEN".equals(current.get().get("status"))) { return "redirect:/shifts"; }
        model.addAttribute("orders", sales.list(actor.getName()));
        model.addAttribute("shift", current.get());
        return "pos/orders";
    }

    @PostMapping("/sales")
    public String create(Principal actor, @RequestParam(defaultValue = "") String requestKey) {
        return "redirect:/sales/" + sales.create(actor.getName(), requestKey);
    }

    @GetMapping("/sales/{id}")
    public String order(Principal actor, @PathVariable long id, @RequestParam(defaultValue = "") String q,
                        @RequestParam(required = false) Long category, @RequestParam(defaultValue = "0") int page, Model model) {
        var order = sales.view(actor.getName(), id);
        if ("DRAFT".equals(order.get("status")) && !Boolean.TRUE.equals(order.get("shift_open"))) {
            return "redirect:/shifts";
        }
        model.addAttribute("order", order);
        model.addAttribute("items", sales.items(actor.getName(), id));
        var products = catalog.search(q, category, true, Math.max(0, page));
        model.addAttribute("products", products.stream().limit(20).toList());
        model.addAttribute("hasNext", products.size() > 20);
        model.addAttribute("page", Math.max(0, page));
        model.addAttribute("categories", catalog.categories());
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        return "pos/sale";
    }

    @PostMapping("/sales/{id}/items")
    public String add(Principal actor, @PathVariable long id, @RequestParam(defaultValue = "") String code) {
        sales.add(actor.getName(), id, code);
        return "redirect:/sales/" + id;
    }

    @PostMapping("/sales/{id}/quantity")
    public String quantity(Principal actor, @PathVariable long id, @RequestParam long itemId,
                           @RequestParam String quantity, @RequestParam(defaultValue = "false") boolean confirmed) {
        sales.quantity(actor.getName(), id, itemId, quantity, confirmed);
        return "redirect:/sales/" + id;
    }

    @GetMapping("/sales/{id}/remove/{itemId}")
    public String removeConfirm(Principal actor, @PathVariable long id, @PathVariable long itemId, Model model) {
        var order = sales.view(actor.getName(), id);
        if (!Boolean.TRUE.equals(order.get("shift_open"))) { return "redirect:/shifts"; }
        if (!"DRAFT".equals(order.get("status"))) { return "redirect:/sales/" + id; }
        var item = sales.items(actor.getName(), id).stream().filter(row -> ((Number) row.get("id")).longValue() == itemId)
                .findFirst().orElseThrow(() -> new BusinessException("Dòng sản phẩm không thuộc đơn hàng."));
        return confirm(model, "Xóa " + item.get("product_name_snapshot") + " khỏi đơn?",
                "/sales/" + id + "/remove", "/sales/" + id, itemId);
    }

    @PostMapping("/sales/{id}/remove")
    public String remove(Principal actor, @PathVariable long id, @RequestParam long itemId,
                         @RequestParam(defaultValue = "false") boolean confirmed) {
        sales.quantity(actor.getName(), id, itemId, "0", confirmed);
        return "redirect:/sales/" + id;
    }

    @GetMapping("/sales/{id}/cancel")
    public String cancelConfirm(Principal actor, @PathVariable long id, Model model) {
        var order = sales.view(actor.getName(), id);
        if (!Boolean.TRUE.equals(order.get("shift_open"))) { return "redirect:/shifts"; }
        if (!"DRAFT".equals(order.get("status"))) { return "redirect:/sales/" + id; }
        model.addAttribute("cancelReason", true);
        return confirm(model, "Hủy đơn hàng đang bán?", "/sales/" + id + "/cancel", "/sales/" + id, null);
    }

    private String confirm(Model model, String title, String action, String back, Long item) {
        model.addAttribute("title", title); model.addAttribute("action", action);
        model.addAttribute("back", back); model.addAttribute("itemId", item);
        return "pos/confirm";
    }

    @PostMapping("/sales/{id}/cancel")
    public String cancel(Principal actor, @PathVariable long id, @RequestParam(defaultValue = "") String reason,
                         @RequestParam(defaultValue = "false") boolean confirmed) {
        sales.cancel(actor.getName(), id, reason, confirmed);
        return "redirect:/sales/" + id;
    }

    @GetMapping("/sales/{id}/checkout")
    public String checkout(Principal actor, @PathVariable long id, Model model) {
        var order = sales.view(actor.getName(), id);
        model.addAttribute("order", order);
        model.addAttribute("items", sales.items(actor.getName(), id));
        model.addAttribute("payments", sales.payments(actor.getName(), id));
        model.addAttribute("invoiceId", sales.invoiceId(actor.getName(), id));
        model.addAttribute("transfer", transfer);
        model.addAttribute("configured", transfer.configured());
        model.addAttribute("qr", transfer.qrUrl((BigDecimal) order.get("total_amount"), order.get("order_no").toString()));
        model.addAttribute("cashKey", UUID.randomUUID().toString());
        model.addAttribute("bankKey", UUID.randomUUID().toString());
        model.addAttribute("failureKey", UUID.randomUUID().toString());
        return "pos/checkout";
    }

    @PostMapping("/sales/{id}/pay")
    public String pay(Principal actor, @PathVariable long id, @ModelAttribute Forms.Payment form, Model model,
                      jakarta.servlet.http.HttpServletResponse response) {
        try {
            return "redirect:/invoices/" + sales.pay(actor.getName(), id, form);
        } catch (BusinessException | DataIntegrityViolationException ex) {
            response.setStatus(400);
            model.addAttribute("error", ex instanceof BusinessException ? ex.getMessage()
                    : "Mã tham chiếu hoặc mã yêu cầu đã được sử dụng. Vui lòng kiểm tra lại giao dịch.");
            model.addAttribute("paymentForm", form);
            return checkout(actor, id, model);
        }
    }

    @PostMapping("/sales/{id}/fail")
    public String failed(Principal actor, @PathVariable long id, @RequestParam String method,
                         @RequestParam String requestKey, RedirectAttributes flash) {
        sales.fail(actor.getName(), id, method, requestKey);
        flash.addFlashAttribute("message", "Đã ghi nhận lần thanh toán thất bại, chưa thu tiền. Có thể thử lại hoặc đổi phương thức.");
        return "redirect:/sales/" + id + "/checkout";
    }

    @GetMapping("/invoices/{id}")
    public String invoice(Principal actor, @PathVariable long id, Model model) {
        Map<String, Object> invoice = sales.invoice(actor.getName(), id);
        model.addAttribute("invoice", invoice);
        return "pos/invoice";
    }
}
