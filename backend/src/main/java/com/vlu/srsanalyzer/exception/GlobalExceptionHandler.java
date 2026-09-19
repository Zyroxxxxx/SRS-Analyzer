package com.vlu.srsanalyzer.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "Dữ liệu không hợp lệ" : error.getDefaultMessage())
                .orElse("Dữ liệu không hợp lệ");
        return ResponseEntity.badRequest().body(message(msg));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, String>> dataIntegrity(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(message("Không thể lưu dữ liệu vì thông tin đang được sử dụng hoặc còn dữ liệu liên quan."));
    }

    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<Map<String, String>> business(RuntimeException e) {
        String msg = e.getMessage() == null ? "Yêu cầu không hợp lệ" : e.getMessage();
        String lower = msg.toLowerCase();
        HttpStatus status;
        if (lower.contains("chưa đăng nhập") || lower.contains("phiên đăng nhập") || lower.contains("sai tài khoản")) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (lower.contains("không có quyền") || lower.contains("chỉ quản trị viên") || lower.contains("bị khóa")) {
            status = HttpStatus.FORBIDDEN;
        } else if (lower.contains("không tìm thấy")) {
            status = HttpStatus.NOT_FOUND;
        } else if (lower.contains("đã tồn tại") || lower.contains("đã được sử dụng") || lower.contains("vượt hạn mức")) {
            status = HttpStatus.CONFLICT;
        } else {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(message(msg));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, String>> unexpected(Exception e) {
        // Do not leak SQL/stack-trace details to the browser.
        return ResponseEntity.internalServerError().body(message("Máy chủ gặp lỗi khi xử lý yêu cầu."));
    }

    private Map<String, String> message(String value) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("message", value == null || value.isBlank() ? "Yêu cầu không hợp lệ" : value);
        return body;
    }
}
