package com.chaoqer.config;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
class AliyunConfiguration {

    @Value("${aliyun.sts.access-key.id}")
    private String stsAccessKeyId;
    @Value("${aliyun.sts.access-key.secret}")
    private String stsAccessKeySecret;
    @Value("${aliyun.sms.access-key.id}")
    private String smsAccessKeyId;
    @Value("${aliyun.sms.access-key.secret}")
    private String smsAccessKeySecret;


    @PostConstruct
    public void init() {
        // 超时时间
        System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
        System.setProperty("sun.net.client.defaultReadTimeout", "10000");
    }

    @Bean
    public IAcsClient stsClient() {
        IClientProfile profile = DefaultProfile.getProfile("cn-shenzhen", stsAccessKeyId, stsAccessKeySecret);
        return new DefaultAcsClient(profile);
    }

    @Bean
    public IAcsClient smsClient() {
        IClientProfile profile = DefaultProfile.getProfile("cn-shenzhen", smsAccessKeyId, smsAccessKeySecret);
        return new DefaultAcsClient(profile);
    }

}
