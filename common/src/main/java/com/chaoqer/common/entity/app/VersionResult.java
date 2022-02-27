package com.chaoqer.common.entity.app;

import lombok.Data;

@Data
public class VersionResult {

    // 版本ID
    private String versionId;
    // 客户端类型
    private int clientType;
    // 版本名
    private String versionName;
    // 版本号
    private int versionCode;
    // 版本描述
    private String versionDesc;
    // 是否强制升级
    private int forcedUpgrade;
    // 下载地址
    private String downloadUrl;
    // 创建时间
    private long createTime;

}
