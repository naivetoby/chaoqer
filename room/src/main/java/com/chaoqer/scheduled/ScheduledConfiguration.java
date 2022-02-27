package com.chaoqer.scheduled;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledConfiguration {

    @Autowired
    private ScheduledTasks scheduledTasks;

    /**
     * 每秒扫描所有发送的表情是否需要清理
     */
    @Scheduled(cron = "* * * * * ?")
    public void roomIconDeleteTaskCron() {
        scheduledTasks.roomIconDeleteTask();
    }

}