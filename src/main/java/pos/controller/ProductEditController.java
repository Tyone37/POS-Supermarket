package pos.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.sql.Timestamp;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pos.dto.ProductEditForm;
import pos.dto.ProductStatusForm;
import pos.exception.BusinessException;
import pos.service.CatalogService;

@Controller
public class ProductEditController {
    private final CatalogService catalog;

    public ProductEditController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @ModelAttribute
    void common(Model model, Authentication authentication) {
        var roles = authentication.getAuthorities().stream().map(Object::toString).toList();
        model.addAttribute("canManager", roles.contains("ROLE_MANAGER"));
        model.addAttribute("canCashier", roles.contains("ROLE_CASHIER"));
        model.addAttribute("canAdmin", roles.contains("ROLE_ADMIN"));
    }

    @GetMapping("/products/{id}/edit")
    public String edit(Principal actor, @PathVariable long id, Model model) {
        var product = catalog.editable(actor.getName(), id);
        var form = new ProductEditForm();
        form.setBarcode((String) product.get("barcode"));
        form.setName((String) product.get("name"));
        form.setCategoryId(((Number) product.get("category_id")).longValue());
        form.setUnitId(((Number) product.get("unit_id")).longValue());
        form.setPrice(((BigDecimal) product.get("sale_price")).stripTrailingZeros().toPlainString());
        form.setStock(((BigDecimal) product.get("stock_quantity")).stripTrailingZeros().toPlainString());
        form.setImageUrl((String) product.get("image_url"));
        form.setExpectedUpdatedAt(((Timestamp) product.get("updated_at")).toInstant().toString());
        model.addAttribute("form", form);
        return page(product, model);
    }

    @PostMapping("/products/{id}")
    public String update(Principal actor, @PathVariable long id, @Valid @ModelAttribute("form") ProductEditForm form,
                         BindingResult errors, Model model, HttpServletResponse response, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                catalog.update(actor.getName(), id, form);
                flash.addFlashAttribute("message", "Đã cập nhật sản phẩm. Hóa đơn đã lập giữ nguyên thông tin.");
                return "redirect:/products";
            } catch (BusinessException exception) {
                model.addAttribute("error", exception.getMessage());
            } catch (DataIntegrityViolationException exception) {
                errors.rejectValue("barcode", "duplicate", "Mã vạch hoặc SKU đã tồn tại. Vui lòng kiểm tra lại.");
            }
        }
        response.setStatus(400);
        return page(catalog.editable(actor.getName(), id), model);
    }

    @PostMapping("/products/{id}/status")
    public String changeStatus(Principal actor, @PathVariable long id,
                               @Valid @ModelAttribute("statusForm") ProductStatusForm form,
                               BindingResult errors, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            throw new BusinessException("Trạng thái bán hoặc phiên bản sản phẩm không hợp lệ. Vui lòng tải lại danh sách.");
        }
        catalog.changeStatus(actor.getName(), id, form);
        flash.addFlashAttribute("message", form.getStatus() == ProductStatusForm.Status.STOPPED
                ? "Đã ngừng bán sản phẩm. Lịch sử giao dịch được giữ nguyên."
                : "Đã kích hoạt sản phẩm. Thu ngân có thể tìm và quét mã để bán.");
        return "redirect:/products";
    }

    private String page(Map<String, Object> product, Model model) {
        model.addAttribute("product", product);
        model.addAttribute("categories", catalog.categories());
        model.addAttribute("units", catalog.units());
        return "pos/product-edit";
    }
}
