package pos.automation;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chrome.ChromeDriver;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class taoTaiKhoan {
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
    void adminTaoTaiKhoan() {
        String username = "test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        driver = browser();
        driver.manage().window().maximize();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        driver.get("http://localhost:" + port + "/login");
        pause();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username"))).sendKeys(System.getenv().getOrDefault("POS_TEST_ADMIN_USERNAME", "admin"));
        pause();

        driver.findElement(By.id("password")).sendKeys(System.getenv().getOrDefault("POS_TEST_LOGIN_PASSWORD", "demo123"));
        pause();

        driver.findElement(By.cssSelector("form button[type='submit']")).click();
        pause();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("form.app-logout")));

        driver.get("http://localhost:" + port + "/accounts");
        pause();
        createAccount(driver, wait, username, "Test1234!");
        assertTrue(driver.findElement(By.cssSelector("tbody[data-filter-rows]")).getText().contains(username));
        System.out.println("Da tao tai khoan kiem thu: " + username);

    }

    private static WebDriver browser() {
        ChromeOptions options = new ChromeOptions();
        if (Boolean.parseBoolean(System.getProperty("automation.headless", "false"))) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1440,1000");
        return new ChromeDriver(options);
    }

    private static void createAccount(WebDriver driver, WebDriverWait wait, String username, String password) {
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-open-dialog='create-record']"))).click();
        pause();
        WebElement dialog = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("create-record")));
        dialog.findElement(By.id("fullName")).sendKeys("Nhan vien kiem thu");
        pause();
        dialog.findElement(By.id("username")).sendKeys(username);
        pause();
        dialog.findElement(By.id("password")).sendKeys(password);
        pause();
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].value = 'CASHIER'; arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                dialog.findElement(By.id("roleCode")));
        pause();
        dialog.findElement(By.cssSelector("button[type='submit']")).click();
        pause();
        wait.until(d -> d.findElements(By.cssSelector("tbody[data-filter-rows] tr")).stream()
                .anyMatch(row -> row.findElements(By.tagName("td")).size() > 1
                        && username.equals(row.findElements(By.tagName("td")).get(1).getText())));
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
