package pos;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:workflow;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema-h2.sql",
        "pos.transfer.bank-bin=970422", "pos.transfer.account-number=123456789",
        "pos.transfer.account-name=CUA HANG THU NGHIEM"})
@AutoConfigureMockMvc
class PosWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired pos.service.SaleService sales;
    @Autowired pos.service.ShiftService shifts;

    @BeforeEach
    void seed() {
        for (String table : new String[]{"inventory_movements", "invoices", "payments", "order_items", "orders",
                "products", "shifts", "employees", "accounts", "role_permissions", "permissions", "roles",
                "product_categories", "units", "registers"}) {
            db.update("DELETE FROM " + table);
        }
        db.update("INSERT INTO roles(id,code,name) VALUES(1,'CASHIER','Thu ngân'),(2,'MANAGER','Quản lý'),(3,'ADMIN','Quản trị')");
        db.update("INSERT INTO accounts(id,username,password_hash,full_name,role_id,status,created_at,updated_at) VALUES"
                + "(1,'cashier','unused','Thu ngân 1',1,'ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),"
                + "(2,'other','unused','Thu ngân 2',1,'ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),"
                + "(3,'manager','unused','Quản lý',2,'ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),"
                + "(4,'admin','unused','Quản trị',3,'ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        db.update("INSERT INTO employees(id,account_id,employee_code) VALUES(1,1,'NV01'),(2,2,'NV02')");
        db.update("INSERT INTO registers(id,code,name) VALUES(1,'Q01','Quầy 1')");
        db.update("INSERT INTO product_categories(id,code,name) VALUES(1,'SUA','Sữa'),(2,'BANH','Bánh')");
        db.update("INSERT INTO units(id,code,name) VALUES(1,'HOP','Hộp')");
        db.update("INSERT INTO products(id,code,barcode,name,category_id,unit_id,sale_price,stock_quantity,created_at,updated_at)"
                + " VALUES(1,'SP01','893001','Sữa tươi',1,1,32000,10,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
    }

    private MockHttpServletRequestBuilder cashier(MockHttpServletRequestBuilder request) {
        return request.with(user("cashier").roles("CASHIER")).with(csrf());
    }

    private String key() { return UUID.randomUUID().toString(); }

    private long order() throws Exception {
        mvc.perform(cashier(post("/shifts")).param("registerId", "1").param("openingCash", "500000")
                .param("openRequestKey", key())).andExpect(status().is3xxRedirection());
        mvc.perform(cashier(post("/sales")).param("requestKey", key())).andExpect(status().is3xxRedirection());
        long id = db.queryForObject("SELECT MAX(id) FROM orders", Long.class);
        mvc.perform(cashier(post("/sales/" + id + "/items")).param("code", "893001"))
                .andExpect(status().is3xxRedirection());
        return id;
    }

    private MockHttpServletRequestBuilder pay(long order, String method, String received, String reference, String key) {
        return cashier(post("/sales/" + order + "/pay")).param("method", method).param("received", received)
                .param("reference", reference).param("requestKey", key).param("verified", "true");
    }

    @Test void cashierCannotCreateProductsOrAccounts() throws Exception {
        mvc.perform(cashier(post("/products"))).andExpect(status().isForbidden());
        mvc.perform(cashier(post("/accounts"))).andExpect(status().isForbidden());
        mvc.perform(cashier(get("/accounts"))).andExpect(status().isForbidden());
    }

    @Test void shiftIsIdempotentAndCashierScoped() throws Exception {
        String key = key();
        for (int i = 0; i < 2; i++) {
            mvc.perform(cashier(post("/shifts")).param("registerId", "1").param("openingCash", "0")
                    .param("openRequestKey", key)).andExpect(status().is3xxRedirection());
        }
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM shifts", Integer.class));
        mvc.perform(get("/shifts").with(user("other").roles("CASHIER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa mở ca")));
    }

    @Test void invalidOpeningCashCreatesNothing() throws Exception {
        for (String amount : new String[]{"-1", "abc", "", "0.5", "999999999999999999999"}) {
            mvc.perform(cashier(post("/shifts")).param("registerId", "1").param("openingCash", amount)
                    .param("openRequestKey", key())).andExpect(status().isBadRequest());
        }
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM shifts", Integer.class));
    }

    @Test void cashCheckoutIsAtomicAndReplayDoesNotDuplicate() throws Exception {
        long id = order();
        mvc.perform(cashier(post("/sales/" + id + "/items")).param("code", "SP01"))
                .andExpect(status().is3xxRedirection());
        String key = key();
        mvc.perform(pay(id, "CASH", "100000", "", key)).andExpect(status().is3xxRedirection());
        mvc.perform(pay(id, "CASH", "100000", "", key)).andExpect(status().is3xxRedirection());
        Map<String,Object> payment = db.queryForMap("SELECT * FROM payments WHERE status='SUCCESS'");
        assertEquals(0, ((BigDecimal) payment.get("change_amount")).compareTo(new BigDecimal("36000")));
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM invoices", Integer.class));
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM inventory_movements", Integer.class));
        assertEquals(0, db.queryForObject("SELECT stock_quantity FROM products WHERE id=1", BigDecimal.class)
                .compareTo(new BigDecimal("8")));
        mvc.perform(pay(id, "CASH", "99999", "", key)).andExpect(status().isBadRequest());
        mvc.perform(cashier(post("/sales/" + id + "/items")).param("code", "SP01"))
                .andExpect(status().isBadRequest());
        long invoice = db.queryForObject("SELECT id FROM invoices", Long.class);
        db.update("UPDATE products SET name='Tên mới',sale_price=99000 WHERE id=1");
        db.update("UPDATE accounts SET full_name='Tên nhân viên mới' WHERE id=1");
        mvc.perform(cashier(get("/invoices/" + invoice))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sữa tươi")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thu ngân 1")));
        mvc.perform(get("/invoices/" + invoice).with(user("other").roles("CASHIER")))
                .andExpect(status().isForbidden());
    }

    @Test void transferRequiresExactAmountFreshReferenceAndAcknowledgment() throws Exception {
        long id = order();
        for (String value : new String[]{"31000", "33000"}) {
            mvc.perform(pay(id, "BANK_TRANSFER", value, "REF001", key())).andExpect(status().isBadRequest());
        }
        mvc.perform(pay(id, "BANK_TRANSFER", "32000", " ", key())).andExpect(status().isBadRequest());
        mvc.perform(pay(id, "BANK_TRANSFER", "32000", "REF001", key()).with(request -> {
            request.setParameter("verified", "false"); return request;
        }))
                .andExpect(status().isBadRequest());
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM invoices", Integer.class));
        String request = key();
        mvc.perform(pay(id, "BANK_TRANSFER", "32000", " ref001 ", request)).andExpect(status().is3xxRedirection());
        mvc.perform(pay(id, "BANK_TRANSFER", "32000", "REF001", request)).andExpect(status().is3xxRedirection());
        assertEquals("REF001", db.queryForObject("SELECT bank_reference FROM payments", String.class));
        assertEquals(1L, db.queryForObject("SELECT confirmed_by FROM payments", Long.class));
        assertNotNull(db.queryForObject("SELECT confirmed_at FROM payments", java.sql.Timestamp.class));
        long next = order();
        mvc.perform(pay(next, "BANK_TRANSFER", "32000", "ref001", key())).andExpect(status().isBadRequest());
        assertEquals("DRAFT", db.queryForObject("SELECT status FROM orders WHERE id=?", String.class, next));
    }

    @Test void failedAttemptCanRetryCashWithoutCountingAsReceipt() throws Exception {
        long id = order();
        mvc.perform(cashier(post("/sales/" + id + "/fail")).param("method", "BANK_TRANSFER")
                .param("requestKey", key())).andExpect(status().is3xxRedirection());
        assertEquals(0, db.queryForObject("SELECT amount_received FROM payments", BigDecimal.class).signum());
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM invoices", Integer.class));
        mvc.perform(pay(id, "CASH", "32000", "", key())).andExpect(status().is3xxRedirection());
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM payments WHERE status='SUCCESS'", Integer.class));
    }

    @Test void insufficientCashClosedShiftAndOtherCashierCannotPay() throws Exception {
        long id = order();
        mvc.perform(pay(id, "CASH", "31999", "", key())).andExpect(status().isBadRequest());
        mvc.perform(post("/sales/" + id + "/pay").with(user("other").roles("CASHIER")).with(csrf())
                .param("method", "CASH").param("received", "32000").param("requestKey", key()))
                .andExpect(status().isForbidden());
        db.update("UPDATE shifts SET status='CLOSED',closed_at=CURRENT_TIMESTAMP");
        mvc.perform(pay(id, "CASH", "32000", "", key())).andExpect(status().isBadRequest());
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM invoices", Integer.class));
    }

    @Test void cancelAndRemoveRequireConfirmationAndKeepHistory() throws Exception {
        long id = order();
        long item = db.queryForObject("SELECT id FROM order_items", Long.class);
        mvc.perform(cashier(post("/sales/" + id + "/quantity")).param("itemId", "" + item).param("quantity", "0"))
                .andExpect(status().isBadRequest());
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM order_items", Integer.class));
        mvc.perform(cashier(post("/sales/" + id + "/cancel")).param("reason", "Khách đổi ý"))
                .andExpect(status().isBadRequest());
        mvc.perform(cashier(post("/sales/" + id + "/cancel")).param("reason", "Khách đổi ý").param("confirmed", "true"))
                .andExpect(status().is3xxRedirection());
        assertEquals("CANCELLED", db.queryForObject("SELECT status FROM orders", String.class));
        assertEquals("Khách đổi ý", db.queryForObject("SELECT cancellation_reason FROM orders", String.class));
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM order_items", Integer.class));
        mvc.perform(pay(id, "CASH", "32000", "", key())).andExpect(status().isBadRequest());
    }

    @Test void searchIsAccentInsensitiveAndCategoryFiltered() throws Exception {
        long id = order();
        mvc.perform(cashier(get("/sales/" + id)).param("q", "sua tuoi").param("category", "1"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("products"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sữa tươi")));
    }

    @Test void managerCreatesProductAndDuplicateBarcodeIsRejected() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/products").with(user("manager").roles("MANAGER")).with(csrf())
                    .param("code", "NEW01").param("barcode", "893002").param("name", "Bánh quy")
                    .param("categoryId", "2").param("unitId", "1").param("price", "18000").param("stock", "20"))
                    .andExpect(i == 0 ? status().is3xxRedirection() : status().isBadRequest());
        }
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM products WHERE barcode='893002'", Integer.class));
    }

    @Test void adminCreatesCashierWithEmployeeAndCanLockIt() throws Exception {
        mvc.perform(post("/accounts").with(user("admin").roles("ADMIN")).with(csrf())
                .param("username", "newcashier").param("password", "SafePassword123!")
                .param("fullName", "Thu ngân mới").param("roleCode", "CASHIER"))
                .andExpect(status().is3xxRedirection());
        long id = db.queryForObject("SELECT id FROM accounts WHERE username='newcashier'", Long.class);
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM employees WHERE account_id=?", Integer.class, id));
        String hash = db.queryForObject("SELECT password_hash FROM accounts WHERE id=?", String.class, id);
        assertTrue(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches("SafePassword123!", hash));
        mvc.perform(post("/accounts/" + id + "/lock").with(user("admin").roles("ADMIN")).with(csrf())
                .param("confirmed", "true")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/shifts").with(user("newcashier").roles("CASHIER")))
                .andExpect(status().is3xxRedirection());
    }

    @Test void invoiceFailureRollsBackReceiptInventoryAndOrder() throws Exception {
        long id = order();
        db.execute("ALTER TABLE invoices ADD CONSTRAINT test_reject_invoice CHECK(total_amount<0)");
        try {
            mvc.perform(pay(id, "BANK_TRANSFER", "32000", "ROLLBACK001", key())).andExpect(status().isBadRequest());
            assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM payments", Integer.class));
            assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM inventory_movements", Integer.class));
            assertEquals("DRAFT", db.queryForObject("SELECT status FROM orders", String.class));
            assertEquals(0, new BigDecimal("10").compareTo(db.queryForObject("SELECT stock_quantity FROM products", BigDecimal.class)));
        } finally {
            db.execute("ALTER TABLE invoices DROP CONSTRAINT test_reject_invoice");
        }
        mvc.perform(pay(id, "BANK_TRANSFER", "32000", "ROLLBACK001", key())).andExpect(status().is3xxRedirection());
    }

    @Test void concurrentCashConfirmationsReturnSameInvoice() throws Exception {
        long id = order();
        String requestKey = key();
        var form = new pos.dto.Forms.Payment("CASH", "32000", null, requestKey, null);
        var gate = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Long> confirm = () -> { gate.await(); return sales.pay("cashier", id, form); };
            var first = executor.submit(confirm);
            var second = executor.submit(confirm);
            gate.countDown();
            assertEquals(first.get(15, java.util.concurrent.TimeUnit.SECONDS), second.get(15, java.util.concurrent.TimeUnit.SECONDS));
        }
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM invoices", Integer.class));
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM payments", Integer.class));
        assertEquals(0, new BigDecimal("9").compareTo(db.queryForObject("SELECT stock_quantity FROM products", BigDecimal.class)));
    }

    @Test void stockChangedSinceCartWasBuiltPreventsPayment() throws Exception {
        long id = order();
        db.update("UPDATE products SET stock_quantity=0 WHERE id=1");
        mvc.perform(pay(id, "CASH", "32000", "", key())).andExpect(status().isBadRequest());
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM payments", Integer.class));
        assertEquals("DRAFT", db.queryForObject("SELECT status FROM orders", String.class));
    }

    @Test void cannotContinueDraftFromClosedShift() throws Exception {
        long id = order();
        db.update("UPDATE shifts SET status='CLOSED',closed_at=CURRENT_TIMESTAMP");
        mvc.perform(cashier(get("/sales/" + id))).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/shifts"));
    }

    @Test void reducingToZeroUsesConfirmationAndRecalculatesTotal() throws Exception {
        long id = order();
        long item = db.queryForObject("SELECT id FROM order_items", Long.class);
        mvc.perform(cashier(get("/sales/" + id + "/remove/" + item))).andExpect(status().isOk());
        mvc.perform(cashier(post("/sales/" + id + "/quantity")).param("itemId", "" + item)
                .param("quantity", "0").param("confirmed", "true")).andExpect(status().is3xxRedirection());
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM order_items", Integer.class));
        assertEquals(0, db.queryForObject("SELECT total_amount FROM orders", BigDecimal.class).signum());
        mvc.perform(pay(id, "CASH", "32000", "", key())).andExpect(status().isBadRequest());
    }

    @Test void unknownCodeDoesNotChangeCart() throws Exception {
        long id = order();
        mvc.perform(cashier(post("/sales/" + id + "/items")).param("code", "unknown"))
                .andExpect(status().isBadRequest());
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM order_items", Integer.class));
        assertEquals(0, new BigDecimal("32000").compareTo(db.queryForObject("SELECT total_amount FROM orders", BigDecimal.class)));
    }

    @Test void cashierPagesRenderNoStoreAndHaveNoAdminLinks() throws Exception {
        mvc.perform(cashier(get("/shifts"))).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("href=\"/accounts\""))));
    }
}
