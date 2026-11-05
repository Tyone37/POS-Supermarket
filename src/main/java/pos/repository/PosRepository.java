package pos.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PosRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public PosRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    private Map<String, Object> params(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put((String) values[i], values[i + 1]);
        }
        return result;
    }

    private List<Map<String, Object>> rows(String sql, Object... values) {
        return jdbc.queryForList(sql, params(values));
    }

    private Optional<Map<String, Object>> one(String sql, Object... values) {
        return rows(sql, values).stream().findFirst();
    }

    private long insert(String sql, Object... values) {
        var key = new GeneratedKeyHolder();
        jdbc.update(sql, new MapSqlParameterSource(params(values)), key, new String[]{"id"});
        return key.getKey().longValue();
    }

    public Optional<Map<String, Object>> account(String username, boolean lock) {
        return one("SELECT a.*,r.code AS role_code FROM accounts a JOIN roles r ON r.id=a.role_id "
                + "WHERE a.username=:username" + (lock ? " FOR UPDATE" : ""), "username", username);
    }

    public Optional<Map<String, Object>> accountById(long id) {
        return one("SELECT * FROM accounts WHERE id=:id FOR UPDATE", "id", id);
    }

    public List<Map<String, Object>> accounts() {
        return rows("SELECT a.id,a.username,a.full_name,a.status,r.code AS role_code FROM accounts a "
                + "JOIN roles r ON r.id=a.role_id ORDER BY a.id DESC");
    }

    public List<Map<String, Object>> roles() { return rows("SELECT * FROM roles ORDER BY id"); }
    public List<Map<String, Object>> categories() {
        return rows("SELECT * FROM product_categories WHERE status='ACTIVE' ORDER BY name");
    }
    public List<Map<String, Object>> units() { return rows("SELECT * FROM units ORDER BY name"); }
    public List<Map<String, Object>> registers() {
        return rows("SELECT * FROM registers WHERE status='ACTIVE' ORDER BY code");
    }

    public long createAccount(String username, String hash, String name, long role, Object now) {
        return insert("""
                INSERT INTO accounts(username,password_hash,full_name,role_id,status,created_at,updated_at)
                VALUES(:username,:hash,:name,:role,'ACTIVE',:now,:now)
                """, "username", username, "hash", hash, "name", name, "role", role, "now", now);
    }

    public void createEmployee(long account, String code) {
        jdbc.update("INSERT INTO employees(account_id,employee_code,status) VALUES(:account,:code,'ACTIVE')",
                params("account", account, "code", code));
    }

    public void lockAccount(long id, Object now) {
        jdbc.update("UPDATE accounts SET status='LOCKED',updated_at=:now WHERE id=:id", params("id", id, "now", now));
    }

    public Optional<Map<String, Object>> employee(long account) {
        return one("SELECT * FROM employees WHERE account_id=:id AND status='ACTIVE' FOR UPDATE", "id", account);
    }

    public Optional<Map<String, Object>> latestShift(long employee) {
        return one("SELECT s.*,r.name AS register_name FROM shifts s JOIN registers r ON r.id=s.register_id "
                + "WHERE employee_id=:id ORDER BY CASE WHEN s.status='OPEN' THEN 0 ELSE 1 END,s.id DESC LIMIT 1", "id", employee);
    }

    public Optional<Map<String, Object>> shiftByKey(String key) {
        return one("SELECT * FROM shifts WHERE open_request_key=:key", "key", key);
    }

    public Optional<Map<String, Object>> shift(long id) {
        return one("SELECT s.*,e.account_id FROM shifts s JOIN employees e ON e.id=s.employee_id "
                + "WHERE s.id=:id FOR UPDATE", "id", id);
    }

    public long createShift(String number, long employee, long register, Object cash, String key, Object now) {
        return insert("""
                INSERT INTO shifts(shift_no,employee_id,register_id,opening_cash,open_request_key,opened_at,status)
                VALUES(:number,:employee,:register,:cash,:key,:now,'OPEN')
                """, "number", number, "employee", employee, "register", register, "cash", cash, "key", key, "now", now);
    }

    public List<Map<String, Object>> products(Long category) {
        return rows("""
                SELECT p.*,c.name AS category_name,u.code AS unit_code,u.name AS unit_name,u.quantity_scale
                FROM products p JOIN product_categories c ON c.id=p.category_id JOIN units u ON u.id=p.unit_id
                WHERE (:category IS NULL OR p.category_id=:category) ORDER BY p.name,p.id
                """, "category", category);
    }

    public Optional<Map<String, Object>> product(long id, boolean lock) {
        return one("SELECT p.*,u.code AS unit_code,u.quantity_scale FROM products p JOIN units u ON u.id=p.unit_id "
                + "WHERE p.id=:id" + (lock ? " FOR UPDATE" : ""), "id", id);
    }

    public List<Map<String, Object>> productsByCode(String code) {
        return rows("SELECT p.*,u.code AS unit_code,u.quantity_scale FROM products p JOIN units u ON u.id=p.unit_id "
                + "WHERE p.status='ACTIVE' AND (p.barcode=:code OR p.code=:code) ORDER BY p.id LIMIT 2", "code", code);
    }

    public long createProduct(String code, String barcode, String name, long category, long unit,
                              Object price, Object stock, String image, Object now) {
        long id = insert("""
                INSERT INTO products(code,barcode,name,category_id,unit_id,sale_price,stock_quantity,image_url,
                                     status,created_at,updated_at)
                VALUES(:code,:barcode,:name,:category,:unit,:price,:stock,:image,'ACTIVE',:now,:now)
                """, "code", code, "barcode", barcode, "name", name, "category", category, "unit", unit,
                "price", price, "stock", stock, "image", image, "now", now);
        if (((java.math.BigDecimal) stock).signum() > 0) {
            jdbc.update("""
                    INSERT INTO inventory_movements(event_key,product_id,product_code_snapshot,
                      movement_type,quantity_delta,reason,created_at)
                    VALUES(:key,:product,:code,'ADJUSTMENT',:quantity,'Tồn đầu khi tạo sản phẩm',:now)
                    """, params("key", "INITIAL-" + id, "product", id, "code", code, "quantity", stock, "now", now));
        }
        return id;
    }

    public Optional<Map<String, Object>> order(long id, boolean lock) {
        return one("SELECT * FROM orders WHERE id=:id" + (lock ? " FOR UPDATE" : ""), "id", id);
    }

    public Optional<Map<String, Object>> orderByKey(String key) {
        return one("SELECT * FROM orders WHERE create_request_key=:key", "key", key);
    }

    public List<Map<String, Object>> orders(long account) {
        return rows("SELECT o.* FROM orders o JOIN shifts s ON s.id=o.shift_id "
                + "JOIN employees e ON e.id=s.employee_id WHERE e.account_id=:id ORDER BY o.id DESC LIMIT 30", "id", account);
    }

    public long createOrder(long shift, String number, String key, Object now) {
        return insert("INSERT INTO orders(shift_id,order_no,create_request_key,created_at) VALUES(:shift,:number,:key,:now)",
                "shift", shift, "number", number, "key", key, "now", now);
    }

    public List<Map<String, Object>> items(long order) {
        return rows("SELECT i.*,u.quantity_scale FROM order_items i JOIN products p ON p.id=i.product_id "
                + "JOIN units u ON u.id=p.unit_id WHERE i.order_id=:id ORDER BY i.product_id", "id", order);
    }

    public void createItem(long order, Map<String, Object> product) {
        jdbc.update("""
                INSERT INTO order_items(order_id,product_id,product_code_snapshot,product_name_snapshot,
                  barcode_snapshot,unit_code_snapshot,quantity,unit_price,line_total)
                VALUES(:order,:product,:code,:name,:barcode,:unit,1,:price,:price)
                """, params("order", order, "product", product.get("id"), "code", product.get("code"),
                "name", product.get("name"), "barcode", product.get("barcode"), "unit", product.get("unit_code"),
                "price", product.get("sale_price")));
    }

    public void quantity(long id, Object quantity, Object total) {
        jdbc.update("UPDATE order_items SET quantity=:quantity,line_total=:total WHERE id=:id",
                params("id", id, "quantity", quantity, "total", total));
    }

    public void removeItem(long id) {
        jdbc.update("DELETE FROM order_items WHERE id=:id", params("id", id));
    }

    public void totals(long order, Object subtotal, Object discount, Object total) {
        jdbc.update("UPDATE orders SET subtotal_amount=:subtotal,discount_amount=:discount,total_amount=:total WHERE id=:id",
                params("id", order, "subtotal", subtotal, "discount", discount, "total", total));
    }

    public void cancel(long order, String reason, Object now) {
        jdbc.update("UPDATE orders SET status='CANCELLED',cancelled_at=:now,cancellation_reason=:reason WHERE id=:id",
                params("id", order, "reason", reason, "now", now));
    }

    public Optional<Map<String, Object>> paymentByKey(String key) {
        return one("SELECT * FROM payments WHERE idempotency_key=:key", "key", key);
    }

    public boolean referenceExists(String reference) {
        return one("SELECT id FROM payments WHERE bank_reference=:reference", "reference", reference).isPresent();
    }

    public long createPayment(long order, String method, String status, Object due, Object received, Object change,
                              String key, String reference, Long actor, Object now) {
        return insert("""
                INSERT INTO payments(order_id,method,status,amount_due,amount_received,change_amount,idempotency_key,
                  bank_reference,confirmed_by,confirmed_at,created_at,completed_at)
                VALUES(:order,:method,:status,:due,:received,:change,:key,:reference,:actor,:confirmed,:now,:now)
                """, "order", order, "method", method, "status", status, "due", due, "received", received,
                "change", change, "key", key, "reference", reference, "actor", actor,
                "confirmed", actor == null ? null : now, "now", now);
    }

    public List<Map<String, Object>> payments(long order) {
        return rows("SELECT * FROM payments WHERE order_id=:id ORDER BY id DESC", "id", order);
    }

    public void deductStock(long product, Object quantity, Object now) {
        jdbc.update("UPDATE products SET stock_quantity=stock_quantity-:quantity,updated_at=:now WHERE id=:id",
                params("id", product, "quantity", quantity, "now", now));
    }

    public void movement(Map<String, Object> item, Object now) {
        jdbc.update("""
                INSERT INTO inventory_movements(event_key,product_id,product_code_snapshot,order_item_id,
                  movement_type,quantity_delta,reason,created_at)
                VALUES(:key,:product,:code,:item,'SALE',:quantity,'Bán hàng tại quầy',:now)
                """, params("key", "SALE-" + item.get("id"), "product", item.get("product_id"),
                "code", item.get("product_code_snapshot"), "item", item.get("id"),
                "quantity", ((java.math.BigDecimal) item.get("quantity")).negate(), "now", now));
    }

    public void markPaid(long order, Object now) {
        jdbc.update("UPDATE orders SET status='PAID',paid_at=:now WHERE id=:id", params("id", order, "now", now));
    }

    public long createInvoice(long order, long payment, long actor, String number, Object now) {
        return insert("""
                INSERT INTO invoices(invoice_no,order_id,payment_id,issued_by,cashier_name_snapshot,issued_at,
                                     subtotal_amount,discount_amount,total_amount)
                SELECT :number,o.id,:payment,:actor,a.full_name,:now,o.subtotal_amount,o.discount_amount,o.total_amount
                FROM orders o JOIN accounts a ON a.id=:actor WHERE o.id=:order
                """, "number", number, "payment", payment, "actor", actor, "now", now, "order", order);
    }

    public Optional<Map<String, Object>> invoiceForOrder(long order) {
        return one("SELECT * FROM invoices WHERE order_id=:id", "id", order);
    }

    public Optional<Map<String, Object>> invoice(long id) {
        return one("""
                SELECT i.*,i.cashier_name_snapshot AS cashier_name,p.method,p.amount_received,p.change_amount,
                       p.bank_reference,p.confirmed_at,p.status AS payment_status,o.shift_id
                FROM invoices i JOIN payments p ON p.id=i.payment_id
                JOIN orders o ON o.id=i.order_id WHERE i.id=:id
                """, "id", id);
    }
}
