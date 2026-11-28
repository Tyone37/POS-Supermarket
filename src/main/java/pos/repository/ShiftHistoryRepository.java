package pos.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import pos.dto.ShiftHistory;
import pos.dto.ShiftInvoice;
import pos.entity.InvoiceEntity;
import pos.entity.ShiftEntity;

@Repository
@Transactional(readOnly = true)
public class ShiftHistoryRepository {
    private static final String SHIFT_SELECT = """
            select s, a.fullName, e.employeeCode, r.name
            from ShiftEntity s, EmployeeEntity e, AccountEntity a, RegisterEntity r
            where s.employeeId = e.id and e.accountId = a.id and s.registerId = r.id
            """;
    private final EntityManager em;

    public ShiftHistoryRepository(EntityManager em) {
        this.em = em;
    }

    public List<ShiftHistory.Row> search(ShiftHistory.Query filter, int limit) {
        var jpql = new StringBuilder(SHIFT_SELECT);
        if (filter.fromInclusive() != null) {
            jpql.append(" and s.openedAt >= :from");
        }
        if (filter.toExclusive() != null) {
            jpql.append(" and s.openedAt < :to");
        }
        if (filter.cashierId() != null) {
            jpql.append(" and s.employeeId = :cashier");
        }
        if (filter.registerId() != null) {
            jpql.append(" and s.registerId = :register");
        }
        if (!filter.status().isEmpty()) {
            jpql.append(" and s.status = :status");
        }
        jpql.append(" order by s.openedAt desc, s.id desc");
        var query = em.createQuery(jpql.toString(), Object[].class);
        if (filter.fromInclusive() != null) {
            query.setParameter("from", filter.fromInclusive());
        }
        if (filter.toExclusive() != null) {
            query.setParameter("to", filter.toExclusive());
        }
        if (filter.cashierId() != null) {
            query.setParameter("cashier", filter.cashierId());
        }
        if (filter.registerId() != null) {
            query.setParameter("register", filter.registerId());
        }
        if (!filter.status().isEmpty()) {
            query.setParameter("status", filter.status());
        }
        return query.setFirstResult(filter.offset()).setMaxResults(limit).getResultList()
                .stream().map(this::row).toList();
    }

    public Optional<ShiftHistory.Detail> detail(long id) {
        return em.createQuery(SHIFT_SELECT + " and s.id = :id", Object[].class).setParameter("id", id)
                .getResultStream().findFirst().map(values -> {
                    var s = (ShiftEntity) values[0];
                    return new ShiftHistory.Detail(row(values), s.getOpeningCash(), s.getClosingRevenue(),
                            s.getClosingCashSales(), s.getClosingCashRefunds(), s.getClosingExpectedCash(),
                            s.getClosingActualCash(), s.getClosingDifference());
                });
    }

    public List<ShiftInvoice> invoices(long shiftId, int offset, int limit) {
        return em.createQuery("""
                select i, p.method from InvoiceEntity i, PaymentEntity p, OrderEntity o
                where i.orderId = o.id and i.paymentId = p.id and p.orderId = o.id and o.shiftId = :shift
                  and i.status = 'ISSUED' and o.status = 'PAID' and p.status = 'SUCCESS'
                order by i.issuedAt desc, i.id desc
                """, Object[].class).setParameter("shift", shiftId).setFirstResult(offset).setMaxResults(limit)
                .getResultList().stream().map(values -> {
                    var i = (InvoiceEntity) values[0];
                    return new ShiftInvoice(i.getId(), i.getInvoiceNo(), i.getCashierNameSnapshot(),
                            i.getIssuedAt(), i.getTotalAmount(), (String) values[1]);
                }).toList();
    }

    public List<ShiftHistory.Choice> cashiers() {
        return em.createQuery("""
                select e.id, a.fullName, e.employeeCode from EmployeeEntity e, AccountEntity a
                where e.accountId = a.id order by a.fullName, e.id
                """, Object[].class).getResultList().stream()
                .map(v -> new ShiftHistory.Choice((Long) v[0], v[1] + " · " + v[2])).toList();
    }

    public List<ShiftHistory.Choice> registers() {
        return em.createQuery("select r.id, r.name, r.code from RegisterEntity r order by r.code, r.id", Object[].class)
                .getResultList().stream()
                .map(v -> new ShiftHistory.Choice((Long) v[0], v[1] + " · " + v[2])).toList();
    }

    private ShiftHistory.Row row(Object[] values) {
        var s = (ShiftEntity) values[0];
        return new ShiftHistory.Row(s.getId(), s.getShiftNo(), s.getStatus(), s.getEmployeeId(),
                (String) values[1], (String) values[2], s.getRegisterId(), (String) values[3],
                s.getOpenedAt(), s.getClosedAt());
    }
}
