package com.company.labor.common.exception;

import com.company.labor.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException ex) {
        // Business rule violations are expected flow — debug level is enough.
        log.debug("business exception: {}", ex.getMessage());
        return Result.fail(ex.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::fieldErrorMessage)
                .collect(Collectors.joining("；"));
        String resolved = message.isBlank() ? "参数校验失败" : message;
        log.debug("validation failed: {}", resolved);
        return Result.fail(resolved);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + "：" + violation.getMessage())
                .collect(Collectors.joining("；"));
        String resolved = message.isBlank() ? "参数校验失败" : message;
        log.debug("constraint violation: {}", resolved);
        return Result.fail(resolved);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleDataIntegrity(DataIntegrityViolationException ex) {
        // DB-level constraint messages can reveal schema details — log the raw
        // cause server-side, return a generic message to the client.
        log.warn("data integrity violation", ex);
        return Result.fail("数据已存在或不符合约束，请检查后重试");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        log.debug("malformed request body: {}", ex.getMessage());
        return Result.fail("请求体格式不正确");
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(LockedException.class)
    public Result<Void> handleLocked(LockedException ex) {
        log.info("login rejected (account locked): {}", ex.getMessage());
        return Result.fail(ex.getMessage());
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(DisabledException.class)
    public Result<Void> handleDisabled(DisabledException ex) {
        log.info("login rejected (account disabled): {}", ex.getMessage());
        return Result.fail(ex.getMessage());
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AuthenticationException.class)
    public Result<Void> handleAuthentication(AuthenticationException ex) {
        // Don't echo Spring's message (can leak whether the account exists).
        log.info("authentication failed: {}", ex.getClass().getSimpleName());
        return Result.fail("账号或密码错误");
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDenied(AccessDeniedException ex) {
        log.info("access denied: {}", ex.getMessage());
        return Result.fail("无权操作");
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        // Never leak raw exception text to clients — log the full stack trace
        // server-side and return a generic message.
        log.error("unhandled exception", ex);
        return Result.fail("服务繁忙，请稍后重试");
    }

    private String fieldErrorMessage(FieldError error) {
        String message = error.getDefaultMessage();
        return error.getField() + "：" + (message == null || message.isBlank() ? "参数不合法" : message);
    }
}
