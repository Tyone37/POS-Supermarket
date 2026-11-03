package pos.exception;

import java.util.Map;

public class FormValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public FormValidationException(Map<String, String> fieldErrors) {
        super("Vui lòng kiểm tra dữ liệu nhập");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
