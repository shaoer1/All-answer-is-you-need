package com.offlineqa.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleValidation(MethodArgumentNotValidException ex) {
        System.out.println("Validation error: " + ex.getMessage());
        ex.printStackTrace();
        String msg = ex.getMessage() == null ? "请求参数不合法" : ex.getMessage().replace("\"", "'");
        return "{\"message\":\"请求参数不合法\",\"detail\":\"" + msg + "\"}";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleError(Exception ex) {
        System.out.println("Internal server error: " + ex.getMessage());
        ex.printStackTrace();
        String msg = ex.getMessage() == null ? "服务器内部错误" : ex.getMessage().replace("\"", "'");
        String type = ex.getClass().getName().replace("\"", "'");
        return "{\"message\":\"" + msg + "\",\"type\":\"" + type + "\"}";
    }
}
