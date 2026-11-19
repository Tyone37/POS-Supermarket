package pos.config;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

@Component("posTime")
public class PosTime {
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    public String format(Object value) {
        if (value instanceof Timestamp timestamp) { return DISPLAY.format(timestamp.toInstant()); }
        return value == null ? "—" : value.toString();
    }
}
