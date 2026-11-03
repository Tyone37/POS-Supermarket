package pos.exception;

import java.util.Map;

public class ProductValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public ProductValidationException(Map<String, String> fieldErrors) {
        super("Vui lòng kiểm tra dữ liệu sản phẩm");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
