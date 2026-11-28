package pos.dto;

import java.util.List;

public final class Forms {
    private Forms() { }

    public record Product(String code, String barcode, String name, Long categoryId, Long unitId,
                          String price, String stock, String imageUrl) { }
    public record NewAccount(String username, String password, String fullName, String roleCode) {
        @Override public String toString() { return "NewAccount[username=" + username + "]"; }
    }
    public record Payment(String method, String received, String reference, String requestKey, Boolean verified) { }
    public record Promotion(String code, String name, String discountPercent,
                            String startsAt, String endsAt, List<Long> productIds) { }
    public record DirectDiscount(String scope, Long itemId, String type, String value,
                                 Boolean confirmed, String managerUsername, String managerPassword) { }
}
