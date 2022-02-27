package com.chaoqer.advice;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.TypeMismatchException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vip.toby.rpc.entity.ServerResult;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 校验异常处理
 */
@RestControllerAdvice
public class GlobalControllerAdvice {

    // 处理 form data方式调用接口校验失败抛出的异常
    @ExceptionHandler(BindException.class)
    public JSONObject bindExceptionHandler(BindException e) {
        ObjectError objectError = e.getBindingResult().getAllErrors().get(0);
        if (TypeMismatchException.ERROR_CODE.equals(objectError.getCode())) {
            return JSON.parseObject(ServerResult.buildFailureMessage(((FieldError) objectError).getField().concat("类型不正确")).toString());
        }
        return JSON.parseObject(ServerResult.buildFailureMessage(objectError.getDefaultMessage()).toString());
    }

    // 处理 json 请求体调用接口校验失败抛出的异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public JSONObject methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        ObjectError objectError = e.getBindingResult().getAllErrors().get(0);
        if (TypeMismatchException.ERROR_CODE.equals(objectError.getCode())) {
            return JSON.parseObject(ServerResult.buildFailureMessage(((FieldError) objectError).getField().concat("类型不正确")).toString());
        }
        return JSON.parseObject(ServerResult.buildFailureMessage(objectError.getDefaultMessage()).toString());
    }

    // 处理单个参数校验失败抛出的异常
    @ExceptionHandler(ConstraintViolationException.class)
    public JSONObject constraintViolationExceptionHandler(ConstraintViolationException e) {
        List<String> collect = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());
        return JSON.parseObject(ServerResult.buildFailureMessage(collect.get(0)).toString());
    }

}
