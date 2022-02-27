package com.chaoqer.common.entity.room;

import com.chaoqer.common.entity.club.ClubResult;
import com.chaoqer.common.entity.user.UserProfileResult;
import lombok.Data;

import java.util.List;

@Data
public class RoomResult {

    // 活动ID
    private String roomId;
    // 创建者
    private String uid;
    // 主题
    private String name;
    // 圈子ID
    private String clubId;
    // 私密
    private int inviteOnly;
    // 创建时间
    private long createTime;
    // 圈子
    private ClubResult clubResult;
    // 当前用户
    private List<UserProfileResult> memberList;
    // 当前用户总数
    private long memberTotal;

}
