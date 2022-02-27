package com.chaoqer.common.entity.base;

import lombok.Data;

@Data
public class ClientInfo {

    // 客户端类型
    private int clientType;
    // 客户端版本信息
    private String versionName;
    private int versionCode;

}
