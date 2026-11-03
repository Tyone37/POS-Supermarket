package pos.dto;

import java.math.BigDecimal;

public record CurrentShiftView(String state, Long id, String shiftNo, String registerName,
        String openedAt, String closedAt, BigDecimal openingCash, String actorName) { }
