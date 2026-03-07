package com.speakmaster.common.handler;

import com.speakmaster.common.constant.LogMessages;
import com.speakmaster.common.dto.Result;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理系统中的各种异常，返回统一的错误响应
 * 
 * @author SpeakMaster
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleBusinessException(BusinessException e) {
        log.error(LogMessages.BUSINESS_EXCEPTION, e.getMsg(), e);
        return Result.error(e.getCode(), e.getMsg());
    }

    /**
     * 处理参数校验异常 (RequestBody)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.error(LogMessages.VALIDATION_EXCEPTION, errorMsg, e);
        return Result.error(ErrorCode.PARAM_INVALID.getCode(), errorMsg);
    }

    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleBindException(BindException e) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.error(LogMessages.VALIDATION_EXCEPTION, errorMsg, e);
        return Result.error(ErrorCode.PARAM_INVALID.getCode(), errorMsg);
    }

    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
        String errorMsg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.error(LogMessages.VALIDATION_EXCEPTION, errorMsg, e);
        return Result.error(ErrorCode.PARAM_INVALID.getCode(), errorMsg);
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        String errorMsg = String.format("缺少必要参数: %s", e.getParameterName());
        log.error(errorMsg, e);
        return Result.error(ErrorCode.PARAM_MISSING.getCode(), errorMsg);
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String errorMsg = String.format("参数类型不匹配: %s", e.getName());
        log.error(errorMsg, e);
        return Result.error(ErrorCode.PARAM_INVALID.getCode(), errorMsg);
    }

    /**
     * 处理请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        String errorMsg = String.format("不支持的请求方法: %s", e.getMethod());
        log.error(errorMsg, e);
        return Result.error(ErrorCode.REQUEST_METHOD_ERROR.getCode(), errorMsg);
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleException(Exception e) {
        log.error(LogMessages.SYSTEM_EXCEPTION, e.getMessage(), e);
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMsg());
    }
}
