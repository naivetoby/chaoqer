package com.chaoqer.entity;

import lombok.Data;

@Data
public class AccountAuth {

    // 账号登录Token
    private String accessToken;
    // 账号秘钥
    private String accessSecret;
    // 账号授权登录过期时间
    private int expireTimeDuration;
    // 当前时间
    private long now;

}
