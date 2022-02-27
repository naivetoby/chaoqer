package com.chaoqer.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 跳过参数校验
 */
@Target({METHOD})
@Retention(RUNTIME)
public @interface ParamVerifySkip {

}
