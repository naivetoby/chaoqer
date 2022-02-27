package com.chaoqer.common.annotation;

import com.chaoqer.common.validation.MobileConstraintValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MobileConstraintValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Documented
public @interface MobileValid {

    String message() default "请输入正确的手机号码";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    // 国家地区码
    String countryCode();

    // 手机号
    String mobile();

}
