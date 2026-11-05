package pos.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import pos.entity.AccountEntity;
import pos.entity.InvoiceEntity;
import pos.entity.ShiftEntity;

/** Read-only presentation data. No changes to sale, payment or shift transactions. */
@ControllerAdvice(assignableTypes = {PosController.class, UiNavigationController.class})
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
        if (!request.getRequestURI().equals(request.getContextPath() + "/shifts")) return;
        var shifts = em.createQuery("select s from ShiftEntity s, EmployeeEntity e where s.employeeId=e.id "
                + "and e.accountId=:id order by case when s.status='OPEN' then 0 else 1 end,s.id desc", ShiftEntity.class)
                .setParameter("id", account.getId()).setMaxResults(1).getResultList();
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        BigDecimal revenue = BigDecimal.ZERO, cash = BigDecimal.ZERO;
        if (!shifts.isEmpty()) {
            var results = em.createQuery("select i,p.method from InvoiceEntity i, PaymentEntity p, OrderEntity o "
                    + "where i.paymentId=p.id and i.orderId=o.id and o.shiftId=:shift and p.status='SUCCESS' "
                    + "order by i.issuedAt desc", Object[].class).setParameter("shift", shifts.getFirst().getId()).getResultList();
            for (var result : results) {
                var invoice = (InvoiceEntity) result[0];
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", invoice.getId()); row.put("invoice_no", invoice.getInvoiceNo());
                row.put("cashier_name", invoice.getCashierNameSnapshot()); row.put("issued_at", invoice.getIssuedAt());
                row.put("total_amount", invoice.getTotalAmount());
                rows.add(row);
                revenue = revenue.add(invoice.getTotalAmount());
                if ("CASH".equals(result[1])) cash = cash.add(invoice.getTotalAmount());
            }
        }
        model.addAttribute("uiShiftInvoices", rows);
        model.addAttribute("uiShiftRevenue", revenue);
        model.addAttribute("uiCashRevenue", cash);
    }
}
