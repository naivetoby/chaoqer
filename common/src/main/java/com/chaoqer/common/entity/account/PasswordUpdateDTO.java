package com.chaoqer.common.entity.account;

import com.chaoqer.common.annotation.MobileValid;
import com.chaoqer.common.entity.base.AuthedDTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@MobileValid(countryCode = "countryCode", mobile = "mobile")
public class PasswordUpdateDTO extends AuthedDTO {

    // 国家地区码
    @NotBlank(message = "国家地区码不能为空")
    private String countryCode;

    // 手机号
    @NotBlank(message = "手机号码不能为空")
    private String mobile;

    // 验证 Token
    @NotBlank(message = "验证Token不能为空")
    private String verifyToken;

    // 密码
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{8,20}$", message = "密码必须同时含有数字和字母，且长度要在8-20位之间")
    private String password;

}
