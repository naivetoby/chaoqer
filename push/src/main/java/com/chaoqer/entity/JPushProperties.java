package com.chaoqer.entity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "jpush")
public class JPushProperties {

    private String appKey;

    private String masterSecret;

    private TimeToLive timeToLive;

    private List<String> whiteUidList;

}
