package com.chaoqer.entity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private List<String> liveUidList;

}
