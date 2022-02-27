package com.chaoqer.common.entity.account;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AccountAuthDTO {

    @NotNull(message = "uid不能为空")
    private String uid;

    @NotNull(message = "token不能为空")
    private String token;

}
