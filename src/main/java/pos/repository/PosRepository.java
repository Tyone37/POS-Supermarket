package pos.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import pos.entity.AccountEntity;
import pos.entity.RoleEntity;
import pos.entity.EmployeeEntity;
import pos.entity.RegisterEntity;
import pos.entity.ShiftEntity;
import pos.entity.ProductCategoryEntity;
import pos.entity.UnitEntity;
import pos.entity.ProductEntity;
import pos.entity.OrderEntity;
import pos.entity.OrderItemEntity;
import pos.entity.PaymentEntity;
import pos.entity.InvoiceEntity;
import pos.entity.InventoryMovementEntity;

@Repository
@Transactional
public class PosRepository {
    private final EntityManager em;

    public PosRepository(EntityManager em) { this.em = em; }

    private <T> TypedQuery<T> query(String jpql, Class<T> type, Object... parameters) {
        TypedQuery<T> query = em.createQuery(jpql, type);
        for (int i = 0; i < parameters.length; i += 2) {
            query.setParameter((String) parameters[i], parameters[i + 1]);
        }
        return query;
    }

    private <T> Optional<T> find(Class<T> type, long id, boolean lock) {
        T entity = em.find(type, id, lock ? LockModeType.PESSIMISTIC_WRITE : LockModeType.NONE);
        if (entity != null && lock) {
            // A prior lookup may have cached this entity before waiting for the database lock.
            em.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
        }
        return Optional.ofNullable(entity);
    }

    private Map<String, Object> row(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put((String) values[i], values[i + 1]);
        }
        return result;
    }

    public Optional<Map<String, Object>> account(String username, boolean lock) {
        var lookup = query("select a from AccountEntity a where a.username=:username", AccountEntity.class,
                "username", username);
        if (lock) {
            lookup.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        }
        return lookup.setMaxResults(1).getResultList().stream().findFirst()
                .flatMap(a -> find(AccountEntity.class, a.getId(), lock)).map(a -> {
                    var result = map(a);
                    result.put("role_code", em.find(RoleEntity.class, a.getRoleId()).getCode());
                    return result;
                });
    }

    public Optional<Map<String, Object>> accountById(long id) {
        return find(AccountEntity.class, id, true).map(this::map);
    }

    public List<Map<String, Object>> accounts() {
        return query("select a,r.code from AccountEntity a, RoleEntity r where a.roleId=r.id order by a.id desc",
                Object[].class).getResultList().stream().map(values -> {
                    var result = map((AccountEntity) values[0]);
                    result.put("role_code", values[1]);
                    return result;
                }).toList();
    }

    public List<Map<String, Object>> roles() {
        return query("select r from RoleEntity r order by r.id", RoleEntity.class).getResultList().stream().map(this::map).toList();
    }
    public List<Map<String, Object>> categories() {
        return query("select c from ProductCategoryEntity c where c.status='ACTIVE' order by c.name",
                ProductCategoryEntity.class).getResultList().stream().map(this::map).toList();
    }
    public List<Map<String, Object>> units() {
        return query("select u from UnitEntity u order by u.name", UnitEntity.class).getResultList().stream().map(this::map).toList();
    }
    public List<Map<String, Object>> registers() {
        return query("select r from RegisterEntity r where r.status='ACTIVE' order by r.code",
                RegisterEntity.class).getResultList().stream().map(this::map).toList();
    }

    public long createAccount(String username, String hash, String name, long role, Object now) {
        var a = new AccountEntity();
        a.setUsername(username); a.setPasswordHash(hash); a.setFullName(name); a.setRoleId(role);
        a.setStatus("ACTIVE"); a.setCreatedAt((Timestamp) now); a.setUpdatedAt((Timestamp) now);
        em.persist(a); em.flush();
        return a.getId();
    }
    public void createEmployee(long account, String code) {
        var e = new EmployeeEntity();
        e.setAccountId(account); e.setEmployeeCode(code); e.setStatus("ACTIVE");
        em.persist(e);
    }
    public void lockAccount(long id, Object now) {
        var a = em.find(AccountEntity.class, id);
        a.setStatus("LOCKED"); a.setUpdatedAt((Timestamp) now);
    }
    public Optional<Map<String, Object>> employee(long account) {
        return query("select e from EmployeeEntity e where e.accountId=:id and e.status='ACTIVE'",
                EmployeeEntity.class, "id", account).setMaxResults(1).getResultList().stream().findFirst()
                .flatMap(e -> find(EmployeeEntity.class, e.getId(), true))
                .filter(e -> "ACTIVE".equals(e.getStatus()) && e.getAccountId() == account).map(this::map);
    }

    public Optional<Map<String, Object>> latestShift(long employee) {
        return query("select s,r.name from ShiftEntity s, RegisterEntity r where s.registerId=r.id and s.employeeId=:id "
                + "order by case when s.status='OPEN' then 0 else 1 end,s.id desc", Object[].class, "id", employee)
                .setMaxResults(1).getResultList().stream().findFirst().map(values -> {
                    var result = map((ShiftEntity) values[0]); result.put("register_name", values[1]); return result;
                });
    }
    public Optional<Map<String, Object>> shiftByKey(String key) {
        return query("select s from ShiftEntity s where s.openRequestKey=:key", ShiftEntity.class, "key", key)
                .setMaxResults(1).getResultList().stream().findFirst().map(this::map);
    }
    public Optional<Map<String, Object>> shift(long id) {
        return find(ShiftEntity.class, id, true).map(s -> {
            var result = map(s);
            result.put("account_id", em.find(EmployeeEntity.class, s.getEmployeeId()).getAccountId());
            return result;
        });
    }
    public long createShift(String number, long employee, long register, Object cash, String key, Object now) {
        var s = new ShiftEntity();
        s.setShiftNo(number); s.setEmployeeId(employee); s.setRegisterId(register); s.setOpeningCash((BigDecimal) cash);
        s.setOpenRequestKey(key); s.setOpenedAt((Timestamp) now); s.setStatus("OPEN");
        em.persist(s); em.flush(); return s.getId();
    }

    public List<Map<String, Object>> products(Long category) {
        return query("select p,c.name,u from ProductEntity p, ProductCategoryEntity c, UnitEntity u "
                + "where p.categoryId=c.id and p.unitId=u.id and (:category is null or p.categoryId=:category) "
                + "order by p.name,p.id", Object[].class, "category", category).getResultList().stream().map(values -> {
                    var result = productRow((ProductEntity) values[0], (UnitEntity) values[2]);
                    result.put("category_name", values[1]); return result;
                }).toList();
    }
    private Map<String, Object> productRow(ProductEntity p, UnitEntity u) {
        var result = map(p); result.put("unit_code", u.getCode()); result.put("unit_name", u.getName());
        result.put("quantity_scale", u.getQuantityScale()); return result;
    }
    public Optional<Map<String, Object>> product(long id, boolean lock) {
        return find(ProductEntity.class, id, lock).map(p -> productRow(p, em.find(UnitEntity.class, p.getUnitId())));
    }
    public List<Map<String, Object>> productsByCode(String code) {
        return query("select p,u from ProductEntity p, UnitEntity u where p.unitId=u.id and p.status='ACTIVE' "
                + "and (p.barcode=:code or p.code=:code) order by p.id", Object[].class, "code", code)
                .setMaxResults(2).getResultList().stream()
                .map(values -> productRow((ProductEntity) values[0], (UnitEntity) values[1])).toList();
    }
    public long createProduct(String code, String barcode, String name, long category, long unit,
            Object price, Object stock, String image, Object now) {
        var p = new ProductEntity();
        p.setCode(code); p.setBarcode(barcode); p.setName(name); p.setCategoryId(category); p.setUnitId(unit);
        p.setSalePrice((BigDecimal) price); p.setStockQuantity((BigDecimal) stock); p.setReservedQuantity(BigDecimal.ZERO);
        p.setImageUrl(image); p.setStatus("ACTIVE"); p.setCreatedAt((Timestamp) now); p.setUpdatedAt((Timestamp) now);
        em.persist(p); em.flush();
        if (((BigDecimal) stock).signum() > 0) {
            var m = new InventoryMovementEntity();
            m.setEventKey("INITIAL-" + p.getId()); m.setProductId(p.getId()); m.setProductCodeSnapshot(code);
            m.setMovementType("ADJUSTMENT"); m.setQuantityDelta((BigDecimal) stock);
            m.setReason("Tồn đầu khi tạo sản phẩm"); m.setCreatedAt((Timestamp) now); em.persist(m);
        }
        return p.getId();
    }

    public Optional<Map<String, Object>> order(long id, boolean lock) {
        return find(OrderEntity.class, id, lock).map(this::map);
    }
    public Optional<Map<String, Object>> orderByKey(String key) {
        return query("select o from OrderEntity o where o.createRequestKey=:key", OrderEntity.class, "key", key)
                .setMaxResults(1).getResultList().stream().findFirst().map(this::map);
    }
    public List<Map<String, Object>> orders(long account) {
        return query("select o from OrderEntity o, ShiftEntity s, EmployeeEntity e "
                + "where o.shiftId=s.id and s.employeeId=e.id and e.accountId=:id order by o.id desc",
                OrderEntity.class, "id", account).setMaxResults(30).getResultList().stream().map(this::map).toList();
    }
    public long createOrder(long shift, String number, String key, Object now) {
        var o = new OrderEntity();
        o.setShiftId(shift); o.setOrderNo(number); o.setCreateRequestKey(key); o.setCreatedAt((Timestamp) now);
        o.setStatus("DRAFT"); o.setSubtotalAmount(BigDecimal.ZERO); o.setDiscountAmount(BigDecimal.ZERO); o.setTotalAmount(BigDecimal.ZERO);
        em.persist(o); em.flush(); return o.getId();
    }
    public List<Map<String, Object>> items(long order) {
        return query("select i,u.quantityScale from OrderItemEntity i, ProductEntity p, UnitEntity u "
                + "where i.productId=p.id and p.unitId=u.id and i.orderId=:id order by i.productId",
                Object[].class, "id", order).getResultList().stream().map(values -> {
                    var result = map((OrderItemEntity) values[0]); result.put("quantity_scale", values[1]); return result;
                }).toList();
    }
    public void createItem(long order, Map<String, Object> product) {
        var i = new OrderItemEntity();
        i.setOrderId(order); i.setProductId(((Number) product.get("id")).longValue());
        i.setProductCodeSnapshot((String) product.get("code")); i.setProductNameSnapshot((String) product.get("name"));
        i.setBarcodeSnapshot((String) product.get("barcode")); i.setUnitCodeSnapshot((String) product.get("unit_code"));
        i.setQuantity(BigDecimal.ONE); i.setUnitPrice((BigDecimal) product.get("sale_price"));
        i.setLineTotal(i.getUnitPrice()); i.setDiscountAmount(BigDecimal.ZERO); em.persist(i);
    }
    public void quantity(long id, Object quantity, Object total) {
        var i = em.find(OrderItemEntity.class, id); i.setQuantity((BigDecimal) quantity); i.setLineTotal((BigDecimal) total);
    }
    public void removeItem(long id) { em.remove(em.find(OrderItemEntity.class, id)); em.flush(); }
    public void totals(long order, Object subtotal, Object discount, Object total) {
        var o = em.find(OrderEntity.class, order);
        o.setSubtotalAmount((BigDecimal) subtotal); o.setDiscountAmount((BigDecimal) discount); o.setTotalAmount((BigDecimal) total);
    }
    public void cancel(long order, String reason, Object now) {
        var o = em.find(OrderEntity.class, order);
        o.setStatus("CANCELLED"); o.setCancelledAt((Timestamp) now); o.setCancellationReason(reason);
    }

    public Optional<Map<String, Object>> paymentByKey(String key) {
        return query("select p from PaymentEntity p where p.idempotencyKey=:key", PaymentEntity.class, "key", key)
                .setMaxResults(1).getResultList().stream().findFirst().map(this::map);
    }
    public boolean referenceExists(String reference) {
        return query("select p.id from PaymentEntity p where p.bankReference=:reference", Long.class, "reference", reference)
                .setMaxResults(1).getResultList().stream().findFirst().isPresent();
    }
    public long createPayment(long order, String method, String status, Object due, Object received, Object change,
            String key, String reference, Long actor, Object now) {
        var p = new PaymentEntity();
        p.setOrderId(order); p.setMethod(method); p.setStatus(status); p.setAmountDue((BigDecimal) due);
        p.setAmountReceived((BigDecimal) received); p.setChangeAmount((BigDecimal) change); p.setIdempotencyKey(key);
        p.setBankReference(reference); p.setConfirmedBy(actor); p.setConfirmedAt(actor == null ? null : (Timestamp) now);
        p.setCreatedAt((Timestamp) now); p.setCompletedAt((Timestamp) now); em.persist(p); em.flush(); return p.getId();
    }
    public List<Map<String, Object>> payments(long order) {
        return query("select p from PaymentEntity p where p.orderId=:id order by p.id desc", PaymentEntity.class, "id", order)
                .getResultList().stream().map(this::map).toList();
    }
    public void deductStock(long product, Object quantity, Object now) {
        var p = em.find(ProductEntity.class, product);
        p.setStockQuantity(p.getStockQuantity().subtract((BigDecimal) quantity)); p.setUpdatedAt((Timestamp) now);
    }
    public void movement(Map<String, Object> item, Object now) {
        var m = new InventoryMovementEntity();
        m.setEventKey("SALE-" + item.get("id")); m.setProductId(((Number) item.get("product_id")).longValue());
        m.setProductCodeSnapshot((String) item.get("product_code_snapshot")); m.setOrderItemId(((Number) item.get("id")).longValue());
        m.setMovementType("SALE"); m.setQuantityDelta(((BigDecimal) item.get("quantity")).negate());
        m.setReason("Bán hàng tại quầy"); m.setCreatedAt((Timestamp) now); em.persist(m);
    }
    public void markPaid(long order, Object now) {
        var o = em.find(OrderEntity.class, order); o.setStatus("PAID"); o.setPaidAt((Timestamp) now);
    }
    public long createInvoice(long order, long payment, long actor, String number, Object now) {
        var o = em.find(OrderEntity.class, order); var a = em.find(AccountEntity.class, actor);
        var i = new InvoiceEntity();
        i.setOrderId(order); i.setPaymentId(payment); i.setIssuedBy(actor); i.setInvoiceNo(number);
        i.setCashierNameSnapshot(a.getFullName()); i.setIssuedAt((Timestamp) now); i.setStatus("ISSUED");
        i.setSubtotalAmount(o.getSubtotalAmount()); i.setDiscountAmount(o.getDiscountAmount()); i.setTotalAmount(o.getTotalAmount());
        em.persist(i); em.flush(); return i.getId();
    }
    public Optional<Map<String, Object>> invoiceForOrder(long order) {
        return query("select i from InvoiceEntity i where i.orderId=:id", InvoiceEntity.class, "id", order)
                .setMaxResults(1).getResultList().stream().findFirst().map(this::map);
    }
    public Optional<Map<String, Object>> invoice(long id) {
        return query("select i,p,o.shiftId from InvoiceEntity i, PaymentEntity p, OrderEntity o "
                + "where i.paymentId=p.id and i.orderId=o.id and i.id=:id", Object[].class, "id", id)
                .setMaxResults(1).getResultList().stream().findFirst().map(values -> {
                    var i = (InvoiceEntity) values[0]; var p = (PaymentEntity) values[1]; var result = map(i);
                    result.put("cashier_name", i.getCashierNameSnapshot()); result.put("method", p.getMethod());
                    result.put("amount_received", p.getAmountReceived()); result.put("change_amount", p.getChangeAmount());
                    result.put("bank_reference", p.getBankReference()); result.put("confirmed_at", p.getConfirmedAt());
                    result.put("payment_status", p.getStatus()); result.put("shift_id", values[2]); return result;
                });
    }

    private Map<String, Object> map(AccountEntity value) {
        return row("id", value.getId(),
                "username", value.getUsername(),
                "full_name", value.getFullName(),
                "role_id", value.getRoleId(),
                "status", value.getStatus(),
                "created_at", value.getCreatedAt(),
                "updated_at", value.getUpdatedAt());
    }

    private Map<String, Object> map(RoleEntity value) {
        return row("id", value.getId(),
                "code", value.getCode(),
                "name", value.getName(),
                "description", value.getDescription());
    }

    private Map<String, Object> map(EmployeeEntity value) {
        return row("id", value.getId(),
                "account_id", value.getAccountId(),
                "employee_code", value.getEmployeeCode(),
                "status", value.getStatus());
    }

    private Map<String, Object> map(RegisterEntity value) {
        return row("id", value.getId(),
                "code", value.getCode(),
                "name", value.getName(),
                "status", value.getStatus());
    }

    private Map<String, Object> map(ShiftEntity value) {
        return row("id", value.getId(),
                "shift_no", value.getShiftNo(),
                "employee_id", value.getEmployeeId(),
                "register_id", value.getRegisterId(),
                "status", value.getStatus(),
                "open_request_key", value.getOpenRequestKey(),
                "opened_at", value.getOpenedAt(),
                "closed_at", value.getClosedAt(),
                "opening_cash", value.getOpeningCash());
    }

    private Map<String, Object> map(ProductCategoryEntity value) {
        return row("id", value.getId(),
                "code", value.getCode(),
                "name", value.getName(),
                "status", value.getStatus());
    }

    private Map<String, Object> map(UnitEntity value) {
        return row("id", value.getId(),
                "code", value.getCode(),
                "name", value.getName(),
                "quantity_scale", value.getQuantityScale());
    }

    private Map<String, Object> map(ProductEntity value) {
        return row("id", value.getId(),
                "code", value.getCode(),
                "barcode", value.getBarcode(),
                "name", value.getName(),
                "category_id", value.getCategoryId(),
                "unit_id", value.getUnitId(),
                "sale_price", value.getSalePrice(),
                "stock_quantity", value.getStockQuantity(),
                "reserved_quantity", value.getReservedQuantity(),
                "image_url", value.getImageUrl(),
                "status", value.getStatus(),
                "stopped_at", value.getStoppedAt(),
                "created_at", value.getCreatedAt(),
                "updated_at", value.getUpdatedAt());
    }

    private Map<String, Object> map(OrderEntity value) {
        return row("id", value.getId(),
                "order_no", value.getOrderNo(),
                "shift_id", value.getShiftId(),
                "status", value.getStatus(),
                "subtotal_amount", value.getSubtotalAmount(),
                "discount_amount", value.getDiscountAmount(),
                "total_amount", value.getTotalAmount(),
                "created_at", value.getCreatedAt(),
                "paid_at", value.getPaidAt(),
                "cancelled_at", value.getCancelledAt(),
                "cancellation_reason", value.getCancellationReason(),
                "create_request_key", value.getCreateRequestKey());
    }

    private Map<String, Object> map(OrderItemEntity value) {
        return row("id", value.getId(),
                "order_id", value.getOrderId(),
                "product_id", value.getProductId(),
                "product_code_snapshot", value.getProductCodeSnapshot(),
                "product_name_snapshot", value.getProductNameSnapshot(),
                "barcode_snapshot", value.getBarcodeSnapshot(),
                "unit_code_snapshot", value.getUnitCodeSnapshot(),
                "quantity", value.getQuantity(),
                "unit_price", value.getUnitPrice(),
                "promotion_id", value.getPromotionId(),
                "promotion_code_snapshot", value.getPromotionCodeSnapshot(),
                "promotion_name_snapshot", value.getPromotionNameSnapshot(),
                "discount_amount", value.getDiscountAmount(),
                "line_total", value.getLineTotal());
    }

    private Map<String, Object> map(PaymentEntity value) {
        return row("id", value.getId(),
                "order_id", value.getOrderId(),
                "method", value.getMethod(),
                "status", value.getStatus(),
                "amount_due", value.getAmountDue(),
                "amount_received", value.getAmountReceived(),
                "change_amount", value.getChangeAmount(),
                "idempotency_key", value.getIdempotencyKey(),
                "bank_reference", value.getBankReference(),
                "confirmed_by", value.getConfirmedBy(),
                "confirmed_at", value.getConfirmedAt(),
                "created_at", value.getCreatedAt(),
                "completed_at", value.getCompletedAt());
    }

    private Map<String, Object> map(InvoiceEntity value) {
        return row("id", value.getId(),
                "invoice_no", value.getInvoiceNo(),
                "order_id", value.getOrderId(),
                "payment_id", value.getPaymentId(),
                "issued_by", value.getIssuedBy(),
                "cashier_name_snapshot", value.getCashierNameSnapshot(),
                "issued_at", value.getIssuedAt(),
                "subtotal_amount", value.getSubtotalAmount(),
                "discount_amount", value.getDiscountAmount(),
                "total_amount", value.getTotalAmount(),
                "status", value.getStatus());
    }


}
