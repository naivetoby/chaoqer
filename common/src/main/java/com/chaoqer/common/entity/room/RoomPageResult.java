package com.chaoqer.common.entity.room;

import com.chaoqer.common.entity.base.PageResult;
import lombok.Data;

@Data
public class RoomPageResult extends PageResult {

    // Room ID
    private String roomId;
    // Room
    private RoomResult roomResult;
    // 创建时间
    private long createTime;

}
