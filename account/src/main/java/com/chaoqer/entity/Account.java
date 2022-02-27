package com.chaoqer.entity;

import lombok.Data;

@Data
public class Account {

    // 用户ID
    private String uid;
    // 用户状态
    private int status;
    // 密码
    private String password;
    // 密码Salt
    private String salt;

}
