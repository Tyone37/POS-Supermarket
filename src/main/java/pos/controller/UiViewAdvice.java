package pos.controller;

import java.security.Principal;
import java.util.Arrays;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import pos.entity.AccountEntity;

/** Read-only presentation data. No changes to sale, payment or shift transactions. */
<<<<<<< HEAD
@ControllerAdvice(assignableTypes = {PosController.class, PromotionController.class, UiNavigationController.class,
        CouponController.class})
=======
@ControllerAdvice(assignableTypes = {PosController.class, UiNavigationController.class, ShiftClosingController.class,
        ShiftHistoryController.class, InvoiceSearchController.class, ProductEditController.class})
>>>>>>> parent of 05ff4cb (revert: restore project to 2dd8a34)
public class UiViewAdvice {
    private final EntityManager em;
    public UiViewAdvice(EntityManager em) { this.em = em; }

    @ModelAttribute
    @Transactional(readOnly = true)
    public void presentation(Principal principal, HttpServletRequest request, Model model) {
        if (principal == null) return;
        var account = em.createQuery("select a from AccountEntity a where a.username=:name", AccountEntity.class)
                .setParameter("name", principal.getName()).getResultStream().findFirst().orElseThrow();
        String name = account.getFullName();
        model.addAttribute("uiDisplayName", name);
        String[] words = name.replaceAll("\\s*\\([^)]*\\)\\s*$", "").trim().split("\\s+");
        model.addAttribute("uiInitials", Arrays.stream(words).skip(Math.max(0, words.length - 2))
                .map(word -> word.substring(0, 1)).collect(Collectors.joining()).toUpperCase());
        if (request.getRequestURI().equals(request.getContextPath() + "/products")) {
            model.addAttribute("uiProductCount", em.createQuery("select count(p) from ProductEntity p", Long.class).getSingleResult());
            model.addAttribute("uiActiveProducts", em.createQuery("select count(p) from ProductEntity p where p.status='ACTIVE'", Long.class).getSingleResult());
            model.addAttribute("uiStoppedProducts", em.createQuery("select count(p) from ProductEntity p where p.status<>'ACTIVE'", Long.class).getSingleResult());
            model.addAttribute("uiLowProducts", em.createQuery("select count(p) from ProductEntity p where p.status='ACTIVE' and p.stockQuantity<10", Long.class).getSingleResult());
        }
    }
}
