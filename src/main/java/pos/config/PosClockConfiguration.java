package pos.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PosClockConfiguration {
    @Bean
    public Clock posClock() {
        return Clock.systemUTC();
    }
}
