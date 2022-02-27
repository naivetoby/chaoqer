package com.chaoqer.common.entity.event;

import com.chaoqer.common.entity.base.PageResult;
import lombok.Data;

@Data
public class EventPageResult extends PageResult {

    // Event ID
    private String eventId;
    // Event
    private EventResult eventResult;
    // 日程时间
    private long eventTime;

}
