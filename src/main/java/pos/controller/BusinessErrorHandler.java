package pos.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import pos.exception.BusinessException;

@ControllerAdvice
public class BusinessErrorHandler {
    @ExceptionHandler({BusinessException.class, DataIntegrityViolationException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String invalid(Exception exception, Model model) {
        model.addAttribute("error", exception instanceof BusinessException ? exception.getMessage()
                : "Dữ liệu không hợp lệ hoặc bị trùng. Vui lòng kiểm tra và thử lại.");
        return "pos/error";
    }
}
