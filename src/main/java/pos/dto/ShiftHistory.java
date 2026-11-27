package pos.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;

/** Read models for management's shift history, without exposing JPA entities. */
public final class ShiftHistory {
    private ShiftHistory() {
    }

    public static class Filter {
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate fromDate;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate toDate;
        @Positive(message = "Thu ngân không hợp lệ.")
        private Long cashierId;
        @Positive(message = "Quầy không hợp lệ.")
        private Long registerId;
        @Pattern(regexp = "|OPEN|CLOSED", message = "Trạng thái ca không hợp lệ.")
        private String status = "";

        public LocalDate getFromDate() {
            return fromDate;
        }

        public void setFromDate(LocalDate fromDate) {
            this.fromDate = fromDate;
        }

        public LocalDate getToDate() {
            return toDate;
        }

        public void setToDate(LocalDate toDate) {
            this.toDate = toDate;
        }

        public Long getCashierId() {
            return cashierId;
        }

        public void setCashierId(Long cashierId) {
            this.cashierId = cashierId;
        }

        public Long getRegisterId() {
            return registerId;
        }

        public void setRegisterId(Long registerId) {
            this.registerId = registerId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public record Query(Timestamp fromInclusive, Timestamp toExclusive, Long cashierId, Long registerId,
                        String status, int offset) {
    }

    public record Choice(long id, String label) {
    }

    public record Options(List<Choice> cashiers, List<Choice> registers) {
        public Options {
            cashiers = List.copyOf(cashiers);
            registers = List.copyOf(registers);
        }
    }

    public record Row(long id, String shiftNo, String status, long cashierId, String cashierName,
                      String employeeCode, long registerId, String registerName,
                      Timestamp openedAt, Timestamp closedAt) {
    }

    public record Detail(Row shift, BigDecimal openingCash, BigDecimal closingRevenue,
                         BigDecimal closingCashSales, BigDecimal closingCashRefunds,
                         BigDecimal closingExpectedCash, BigDecimal closingActualCash,
                         BigDecimal closingDifference) {
    }

    public record Page<T>(List<T> items, int number, boolean hasNext) {
        public Page {
            items = List.copyOf(items);
        }
    }

    public record SearchResult(Page<Row> shifts, Options options) {
    }

    public record DetailResult(Detail detail, Page<ShiftInvoice> invoices) {
    }
}
