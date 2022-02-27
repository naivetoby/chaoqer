package com.chaoqer.common.entity.user;

import lombok.Data;

@Data
public class UserProfileResult {

    // 用户ID
    private String uid;
    // 名字
    private String nickname;
    // 头像
    private String avatar;
    // 虚拟形象
    private String figure;
    // 介绍
    private String bio;
    // 是否拉黑
    private int blocked;
    // 是否已发送名片
    private int sendCard;
    // 名片夹总数
    private long cardTotal;
    // 是否开启live功能
    private int allowLive;

}
