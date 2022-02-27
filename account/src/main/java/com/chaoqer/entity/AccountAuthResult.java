package com.chaoqer.entity;

import lombok.Data;

@Data
public class AccountAuthResult {

    // 用户ID
    private String uid;
    // 账号登录Token
    private String accessToken;
    // 账号秘钥
    private String accessSecret;
    // 账号授权登录过期时间
    private long expireTime;

}
