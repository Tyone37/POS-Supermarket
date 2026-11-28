package pos.repository;

import java.util.List;
import java.util.Locale;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pos.dto.InvoiceSearch;
import pos.entity.InvoiceEntity;

@Repository
@Transactional(readOnly = true)
public class InvoiceSearchRepository {
    private final EntityManager em;

    public InvoiceSearchRepository(EntityManager em) {
        this.em = em;
    }

    public List<InvoiceSearch.Row> search(InvoiceSearch.Query filter, int limit) {
        var jpql = new StringBuilder("""
                select i, p.method from InvoiceEntity i, PaymentEntity p
                where i.paymentId = p.id and i.orderId = p.orderId
                """);
        if (filter.issuedBy() != null) {
            jpql.append(" and i.issuedBy = :issuer");
        }
        if (!filter.code().isEmpty()) {
            jpql.append(" and lower(i.invoiceNo) like :code escape '!'");
        }
        if (!filter.method().isEmpty()) {
            jpql.append(" and p.method = :method");
        }
        if (!filter.status().isEmpty()) {
            jpql.append(" and i.status = :status");
        }
        jpql.append(" order by i.issuedAt desc, i.id desc");
        var query = em.createQuery(jpql.toString(), Object[].class);
        if (filter.issuedBy() != null) {
            query.setParameter("issuer", filter.issuedBy());
        }
        if (!filter.code().isEmpty()) {
            // Search user input literally, including SQL LIKE wildcard characters.
            String code = filter.code().toLowerCase(Locale.ROOT)
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_");
            query.setParameter("code", "%" + code + "%");
        }
        if (!filter.method().isEmpty()) {
            query.setParameter("method", filter.method());
        }
        if (!filter.status().isEmpty()) {
            query.setParameter("status", filter.status());
        }
        return query.setFirstResult(filter.offset()).setMaxResults(limit).getResultList().stream()
                .map(values -> {
                    var invoice = (InvoiceEntity) values[0];
                    return new InvoiceSearch.Row(invoice.getId(), invoice.getInvoiceNo(), invoice.getIssuedAt(),
                            invoice.getCashierNameSnapshot(), (String) values[1], invoice.getStatus(),
                            invoice.getTotalAmount());
                }).toList();
    }
}
