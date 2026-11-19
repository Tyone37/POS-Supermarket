package pos.automation;

import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chrome.ChromeDriver;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class timSanPham {
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
    void cashierTimSanPham() {
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

        ensureOpenShift(driver, port);

        assertFalse(driver.getCurrentUrl().contains("/shifts"),
                "Tài khoản cashier cần có ca đang mở trước khi kiểm thử.");

        By searchField = By.cssSelector("form[data-product-search] input[name='q']");
        WebElement newOrderForm = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("main form[action$='/sales']")));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].requestSubmit(arguments[0].querySelector('button[type=submit]'));", newOrderForm);
        pause();
        WebElement search = wait.until(ExpectedConditions.visibilityOfElementLocated(searchField));

        js.executeScript("arguments[0].focus(); arguments[0].value = '';", search);
        pause();
        for (char character : "SP001".toCharArray()) {
            js.executeScript("arguments[0].value += arguments[1]; "
                    + "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));",
                    search, String.valueOf(character));
            pause();

        }
        assertTrue("SP001".equals(search.getDomProperty("value")),
                "Không nhập được mã SP001 vào ô tìm sản phẩm.");

        try {
            wait.until(d -> d.getCurrentUrl().contains("q=SP001"));
        } catch (TimeoutException e) {
            String status = driver.findElement(By.id("product-search-status")).getText();
            throw new AssertionError("Tìm kiếm chưa hoàn tất. Nội dung ô tìm: "
                    + driver.findElement(searchField).getDomProperty("value")
                    + "; trạng thái: " + (status.isBlank() ? "(trống)" : status), e);
        }
        assertTrue(driver.findElements(By.cssSelector("#product-results .product-card")).stream()
                        .anyMatch(product -> "SP001".equals(product.findElement(By.cssSelector("p.muted")).getText().split(" ", 2)[0])),
                "Không tìm thấy sản phẩm SP001 đang hoạt động trong dữ liệu.");

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
