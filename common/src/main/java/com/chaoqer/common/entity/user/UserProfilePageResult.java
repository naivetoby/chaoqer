package com.chaoqer.common.entity.user;

import com.chaoqer.common.entity.base.PageResult;
import lombok.Data;

@Data
public class UserProfilePageResult extends PageResult {

    // 用户 ID
    private String uid;
    // 用户信息
    private UserProfileResult userProfileResult;
    // 创建时间
    private long createTime;

}
