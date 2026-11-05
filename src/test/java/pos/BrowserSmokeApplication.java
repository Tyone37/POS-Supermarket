package pos;

import org.springframework.boot.SpringApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Disposable, loopback-only browser fixture. Never packaged in the application JAR. */
public final class BrowserSmokeApplication {
    private BrowserSmokeApplication() { }

    public static void main(String[] args) {
        var context = SpringApplication.run(PosApplication.class,
                "--server.address=127.0.0.1", "--server.port=18080",
                "--spring.datasource.url=jdbc:h2:mem:browser_smoke;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "--spring.datasource.driver-class-name=org.h2.Driver",
                "--spring.datasource.username=sa", "--spring.datasource.password=",
                "--spring.sql.init.mode=always", "--spring.sql.init.schema-locations=classpath:schema-h2.sql",
                "--pos.transfer.bank-bin=", "--pos.transfer.account-number=", "--pos.transfer.account-name=");
        var db = context.getBean(JdbcTemplate.class);
        var fixtures = new PosWorkflowTest();
        fixtures.db = db;
        fixtures.seed();
        db.update("UPDATE accounts SET password_hash=?",
                context.getBean(PasswordEncoder.class).encode("PosSmoke!2026"));
    }
}
