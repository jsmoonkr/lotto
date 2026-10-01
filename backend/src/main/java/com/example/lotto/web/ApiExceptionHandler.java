package com.example.lotto.web;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String BAD_REQUEST = "요청 값이 올바르지 않습니다.";

    /** 화면에 그대로 보여 줄 수 있도록 첫 번째 필드 오류 메시지를 detail에 담는다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalidBody(MethodArgumentNotValidException e) {
        FieldError error = e.getBindingResult().getFieldError();
        String message = error != null && error.getDefaultMessage() != null && !error.getDefaultMessage().startsWith("{")
                ? error.getDefaultMessage() : BAD_REQUEST;
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({HandlerMethodValidationException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class})
    public ProblemDetail badRequest(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, BAD_REQUEST);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> status(ResponseStatusException e) {
        ProblemDetail body = ProblemDetail.forStatus(e.getStatusCode());
        body.setDetail(e.getReason());
        return ResponseEntity.status(e.getStatusCode()).headers(e.getHeaders()).body(body);
    }

    @ExceptionHandler(RestClientException.class)
    public ProblemDetail upstream(RestClientException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "동행복권 서버에서 데이터를 가져오지 못했습니다.");
    }
}
