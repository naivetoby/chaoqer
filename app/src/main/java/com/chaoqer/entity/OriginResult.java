package com.chaoqer.entity;

import lombok.Data;

@Data
public class OriginResult {

    // 来源ID
    private String originId;
    // 来源信息的作者
    private String uid;
    // 来源信息内容
    private String content;
    // 来源创建时间
    private long createTime;

}
