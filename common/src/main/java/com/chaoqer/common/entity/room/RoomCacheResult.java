package com.chaoqer.common.entity.room;

import com.chaoqer.common.entity.user.UserProfileResult;
import lombok.Data;

@Data
public class RoomCacheResult extends RoomResult {

    // 活动CacheID
    private String roomCacheId;
    // 分享者
    private UserProfileResult fromMember;
    // 是否关闭
    private int closed;

}
