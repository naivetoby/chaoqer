package com.chaoqer.entity;

import lombok.Data;


@Data
public class AppSmsLog {

    // 短信内容
    private String content;
    // 请求内容
    private String reqData;
    // 响应内容
    private String resData;

}
