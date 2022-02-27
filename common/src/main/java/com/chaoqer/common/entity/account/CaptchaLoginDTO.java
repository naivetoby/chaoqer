package com.chaoqer.common.entity.account;

import com.chaoqer.common.annotation.MobileValid;
import com.chaoqer.common.entity.base.AuthedDTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@MobileValid(countryCode = "countryCode", mobile = "mobile")
public class CaptchaLoginDTO extends AuthedDTO {

    // 国家地区码
    @NotBlank(message = "国家地区码不能为空")
    private String countryCode;

    // 手机号
    @NotBlank(message = "手机号码不能为空")
    private String mobile;

    // 短信验证码
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{4,6}$", message = "验证码格式不正确")
    private String captcha;

}
