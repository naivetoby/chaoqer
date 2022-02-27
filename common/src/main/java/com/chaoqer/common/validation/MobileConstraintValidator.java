package com.chaoqer.common.validation;

import com.chaoqer.common.annotation.MobileValid;
import com.chaoqer.common.util.RegexUtil;
import org.springframework.beans.BeanWrapperImpl;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * 判断手机号格式是否合法
 */
public class MobileConstraintValidator implements ConstraintValidator<MobileValid, Object> {

    private String countryCode;
    private String mobile;

    @Override
    public void initialize(MobileValid constraintAnnotation) {
        countryCode = constraintAnnotation.countryCode();
        mobile = constraintAnnotation.mobile();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        BeanWrapperImpl wrapper = new BeanWrapperImpl(value);
        Object countryCodeValue = wrapper.getPropertyValue(countryCode);
        Object mobileValue = wrapper.getPropertyValue(mobile);
        if (countryCodeValue != null && mobileValue != null) {
            return RegexUtil.isValidMobile(countryCodeValue.toString(), mobileValue.toString());
        }
        return false;
    }

}
