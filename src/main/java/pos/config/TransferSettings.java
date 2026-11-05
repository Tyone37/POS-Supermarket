package pos.config;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class TransferSettings {
    private final String bankBin;
    private final String accountNumber;
    private final String accountName;

    public TransferSettings(@Value("${pos.transfer.bank-bin:970422}") String bankBin,
                            @Value("${pos.transfer.account-number:012311234}") String accountNumber,
                            @Value("${pos.transfer.account-name:NGUYEN THANH TOAN}") String accountName) {
        this.bankBin = bankBin.strip();
        this.accountNumber = accountNumber.strip();
        this.accountName = accountName.strip();
    }

    public boolean configured() {
        return bankBin.matches("[0-9]{6}") && accountNumber.matches("[0-9]{6,34}")
                && !accountName.isBlank() && accountName.length() <= 100;
    }

    public String getBankBin() { return bankBin; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountName() { return accountName; }

    public String qrUrl(BigDecimal amount, String orderNumber) {
        if (!configured()) { return null; }
        // The supplied QR is static: the customer enters the amount and order reference.
        if ("970422".equals(bankBin) && "012311234".equals(accountNumber)
                && "NGUYEN THANH TOAN".equals(accountName)) {
            return "/img/payment/transfer-qr.png";
        }
        return UriComponentsBuilder.fromUriString("https://img.vietqr.io/image/" + bankBin + "-"
                + accountNumber + "-compact2.svg")
                .queryParam("amount", amount.toBigIntegerExact().toString())
                .queryParam("addInfo", orderNumber.replace("-", ""))
                .queryParam("accountName", accountName).build().encode().toUriString();
    }
}
