package com.chaoqer.entity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "room")
public class RoomProperties {

    private List<String> automatorUidList;

}
