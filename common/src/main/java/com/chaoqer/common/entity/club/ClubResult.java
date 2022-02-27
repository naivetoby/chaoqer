package com.chaoqer.common.entity.club;

import lombok.Data;

@Data
public class ClubResult {

    // 圈子ID
    private String clubId;
    // 创建者
    private String uid;
    // 名称
    private String name;
    // 封面
    private String cover;
    // 创建时间
    private long createTime;
    // 成员人数
    private long memberTotal;
    // 是否在圈子里面
    private int joined;

}