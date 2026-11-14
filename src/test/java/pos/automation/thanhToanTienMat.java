package pos.automation;

import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chrome.ChromeDriver;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class thanhToanTienMat {
    @LocalServerPort
    private int port;
    private WebDriver driver;

    @AfterEach
    void closeBrowser() {
        if (driver != null) {
            pause();
            driver.quit();
        }
    }

    @Test
    void kiemThuThanhToan() {
        driver = browser();
        driver.manage().window().maximize();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        driver.get("http://localhost:" + port + "/login");
        pause();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username"))).sendKeys(System.getenv().getOrDefault("POS_TEST_CASHIER_USERNAME", "cashier"));
        pause();

        driver.findElement(By.id("password")).sendKeys(System.getenv().getOrDefault("POS_TEST_LOGIN_PASSWORD", "demo123"));
        pause();

        driver.findElement(By.cssSelector("form button[type='submit']")).click();
        pause();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("form.app-logout")));

        // Create a separate order instead of paying an existing draft.
        ensureOpenShift(driver, port);

        assertFalse(driver.getCurrentUrl().contains("/shifts"),
                "Tài khoản cashier cần có ca đang mở trước khi kiểm thử.");
        WebElement newOrderForm = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("main form[action$='/sales']")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].requestSubmit(arguments[0].querySelector('button[type=submit]'));", newOrderForm);
        pause();
        WebElement search;
        try {
            search = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("form[data-product-search] input[name='q']")));
        } catch (TimeoutException e) {
            String message = driver.findElements(By.cssSelector("[role='alert'], main .notice.danger"))
                    .stream().filter(WebElement::isDisplayed).map(WebElement::getText)
                    .filter(text -> !text.isBlank()).findFirst().orElse("Không có thông báo lỗi trên trang.");
            throw new AssertionError("Không mở được đơn mới. Trang: " + driver.getCurrentUrl()
                    + "; thông báo: " + message, e);
        }

        js.executeScript("arguments[0].focus(); arguments[0].value = '';", search);
        pause();
        for (char character : "SP001".toCharArray()) {
            js.executeScript("arguments[0].value += arguments[1]; "
                    + "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));",
                    search, String.valueOf(character));
            pause();

        }
        wait.until(d -> d.getCurrentUrl().contains("q=SP001"));
        WebElement product = wait.until(d -> d.findElements(By.cssSelector("#product-results .product-card"))
                .stream().filter(card -> "SP001".equals(card.findElement(By.cssSelector("p.muted")).getText().split(" ", 2)[0])).findFirst().orElse(null));

        String productName = product.findElement(By.tagName("h3")).getText();
        WebElement addButton = product.findElement(By.cssSelector("button[type='submit']"));
        assertTrue(addButton.isEnabled(), "Sản phẩm SP001 đã hết hàng.");
        String barcode = product.findElement(By.cssSelector("input[name='code']")).getDomProperty("value");
        WebElement scan = driver.findElement(By.id("scan"));
        js.executeScript("arguments[0].value = arguments[1]; "
                + "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));", scan, barcode);
        pause();

        assertTrue(barcode.equals(scan.getDomProperty("value")), "Không nhập được mã vạch vào ô quét.");
        WebElement scanForm = driver.findElement(By.cssSelector("form.scan"));
        js.executeScript("arguments[0].requestSubmit(arguments[0].querySelector('button[type=submit]'));", scanForm);
        pause();
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".cart-items .cart-line")));
        } catch (TimeoutException e) {
            String notice = driver.findElements(By.cssSelector("[data-cart-notice], main [role='alert']"))
                    .stream().filter(WebElement::isDisplayed).map(WebElement::getText)
                    .filter(text -> !text.isBlank()).findFirst().orElse("Không có thông báo từ trang.");
            throw new AssertionError("Không thấy sản phẩm trong giỏ sau khi thêm. " + notice, e);
        }
        WebElement checkoutLink = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".cart-bottom a[href$='/checkout']")));
        driver.get(checkoutLink.getDomProperty("href"));
        pause();
        WebElement cashForm = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("cash-form")));

        String amount = new java.math.BigDecimal(cashForm.getDomAttribute("data-total"))
                .setScale(0, java.math.RoundingMode.CEILING).toPlainString();
        WebElement cashReceived = driver.findElement(By.id("cashReceived"));
        js.executeScript("arguments[0].focus(); arguments[0].value = arguments[1]; "
                + "arguments[0].dispatchEvent(new Event('input', {bubbles: true})); "
                + "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));", cashReceived, amount);
        pause();
        String displayedAmount = cashReceived.getDomProperty("value");
        assertTrue(displayedAmount != null && amount.equals(displayedAmount.replace(".", "").replace(" ", "")),
                "Không nhập được tổng tiền vào ô tiền khách đưa; giá trị hiển thị: " + displayedAmount);

        js.executeScript("arguments[0].requestSubmit(arguments[0].querySelector('button[type=submit]'));", cashForm);
        pause();
        wait.until(ExpectedConditions.urlContains("/invoices/"));
        WebElement receipt = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".receipt-paper")));
        assertEquals("Đã thanh toán", receipt.findElement(By.cssSelector(".receipt-header .badge")).getText());
        assertEquals("Tiền mặt", receipt.findElement(By.xpath(".//dt[normalize-space()='Phương thức:']/following-sibling::dd[1]")).getText());
        assertEquals("Đã phát hành hợp lệ", receipt.findElement(By.xpath(".//dt[normalize-space()='Trạng thái hóa đơn:']/following-sibling::dd[1]")).getText());
        var invoiceItems = receipt.findElements(By.cssSelector("tbody tr"));
        assertEquals(1, invoiceItems.size(), "Hóa đơn phải có đúng một dòng hàng.");
        WebElement invoiceItem = invoiceItems.get(0);
        assertEquals(productName, invoiceItem.findElement(By.cssSelector("td strong")).getText());
        assertEquals("SP001", invoiceItem.findElement(By.cssSelector("td small")).getText());
        assertEquals(0, new java.math.BigDecimal(invoiceItem.findElements(By.tagName("td")).get(1).getText())
                .compareTo(java.math.BigDecimal.ONE), "Số lượng trên hóa đơn phải là 1.");
        String invoiceTotal = receipt.findElement(By.xpath(".//dt[normalize-space()='TỔNG THANH TOÁN:']/following-sibling::dd[1]//span[@class='money-value']")).getText();
        assertEquals(0, new java.math.BigDecimal(invoiceTotal.replace(".", "").replace(",", ""))
                .compareTo(new java.math.BigDecimal(amount)), "Tổng hóa đơn phải khớp số tiền đã thu.");

    }

    private static WebDriver browser() {
        ChromeOptions options = new ChromeOptions();
        if (Boolean.parseBoolean(System.getProperty("automation.headless", "false"))) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1440,1000");
        return new ChromeDriver(options);
    }

    private static void ensureOpenShift(WebDriver driver, int port) {
        driver.get("http://localhost:" + port + "/sales");
        pause();
        if (!driver.getCurrentUrl().contains("/shifts")) return;
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-open-dialog='open-shift']"))).click();
        pause();
        WebElement form = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#open-shift form")));
        WebElement register = form.findElement(By.id("registerId"));
        String registerId = System.getenv().getOrDefault("POS_TEST_REGISTER_ID", "");
        if (registerId.isBlank()) {
            registerId = register.findElements(By.tagName("option")).stream()
                    .map(option -> option.getDomAttribute("value"))
                    .filter(value -> value != null && !value.isBlank()).findFirst()
                    .orElseThrow(() -> new AssertionError("Cần ít nhất một quầy đang hoạt động trong DB kiểm thử."));
        }
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value=arguments[1]; arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
                register, registerId);
        pause();
        form.findElement(By.id("openingCash")).sendKeys("0");
        pause();
        form.findElement(By.cssSelector("button[type='submit']")).click();
        pause();
        wait.until(ExpectedConditions.stalenessOf(form));
        assertFalse(driver.getCurrentUrl().contains("/shifts"), "Không mở được ca. Kiểm tra quầy và hồ sơ cashier trong DB kiểm thử.");
    }

    // Keep each action visible when demonstrating the browser tests.
    private static void pause() {
        long delay = Math.max(0L, Long.getLong("automation.delay.ms", 1500L));
        try {
            Thread.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Automation interrupted during step delay.", exception);
        }
    }
}
