package com.chaoqer.common.entity.event;

import com.chaoqer.common.entity.room.RoomResult;
import com.chaoqer.common.entity.user.UserProfileResult;
import lombok.Data;

import java.util.List;

@Data
public class EventResult {

    // 日程ID
    private String eventId;
    // 创建者
    private String uid;
    // 标题
    private String name;
    // 介绍
    private String desc;
    // 主持人或者嘉宾列表
    private List<String> memberUidList;
    private List<UserProfileResult> memberList;
    // 日程时间
    private long eventTime;
    // 相关活动
    private String roomId;
    private RoomResult roomResult;
    // 创建时间
    private long createTime;
    // 是否已预约提醒
    private int notify;

    public int getEventTimeStatus() {
        long now = System.currentTimeMillis();
        if (now > eventTime + 30 * 60 * 1000) {
            return 2;
        }
        if (now > eventTime - 30 * 60 * 1000) {
            return 1;
        }
        return 0;
    }

}
