package com.chaoqer.server;

import com.alibaba.fastjson.JSONObject;
import com.chaoqer.common.entity.app.LinkDTO;
import com.chaoqer.common.util.DigestUtil;
import com.chaoqer.common.util.HttpClientUtil;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import vip.toby.rpc.annotation.RpcServer;
import vip.toby.rpc.annotation.RpcServerMethod;
import vip.toby.rpc.entity.OperateStatus;
import vip.toby.rpc.entity.RpcType;
import vip.toby.rpc.entity.ServerResult;

import java.util.ArrayList;
import java.util.List;

@RpcServer(value = "app-link-preview", type = RpcType.ASYNC, threadNum = 4)
public class AppLinkPreviewServer {

    private final static Logger logger = LoggerFactory.getLogger(AppLinkPreviewServer.class);

    @Value("${app.link-preview.key}")
    private String linkPreviewKey;
    @Value("${app.link-preview.site}")
    private String linkPreviewSite;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @RpcServerMethod
    public ServerResult postLink(@Validated LinkDTO linkDTO) {
        String link = linkDTO.getLink();
        // 原始link的MD5值
        String linkMD5 = DigestUtil.md5(link);
        // 去掉收尾空白字符串
        link = link.trim();
        // 自动增加前缀
        if (!link.startsWith("http://") && !link.startsWith("https://")) {
            link = "http://".concat(link);
        }
        // 长度是否太长
        if (link.length() > 300) {
            logger.warn("link length > 300, link: {}", link);
            return ServerResult.build(OperateStatus.SUCCESS);
        }
        // 校验是否合法
        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator(schemes);
        if (!urlValidator.isValid(link)) {
            logger.warn("link is Not Valid, link: {}", link);
            return ServerResult.build(OperateStatus.SUCCESS);
        }
        // 缓存是否存在
        String appLinkPreviewKey = RedisKeyGenerator.getAppLinkPreviewKey(linkMD5);
        if (RedisUtil.isKeyExist(redisTemplate, appLinkPreviewKey)) {
            logger.warn("link is Cache Exist, link: {}", link);
            return ServerResult.build(OperateStatus.SUCCESS);
        }
        // 调用API
        List<BasicNameValuePair> inputs = new ArrayList<>();
        inputs.add(new BasicNameValuePair("key", linkPreviewKey));
        inputs.add(new BasicNameValuePair("q", link));
        String result = HttpClientUtil.post(linkPreviewSite, null, inputs);
        if (StringUtils.isNotBlank(result)) {
            logger.info("link preview, link: {}, result: {}", link, result);
            JSONObject resultJSON = null;
            try {
                resultJSON = JSONObject.parseObject(result);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            if (resultJSON != null && StringUtils.isNotBlank(resultJSON.getString("title")) && StringUtils.isNotBlank(resultJSON.getString("url"))) {
                RedisUtil.setObject(redisTemplate, appLinkPreviewKey, result);
                logger.info("link preview Cached, link: {}, linkMD5: {}", link, linkMD5);
            } else {
                logger.info("link preview Skip, link: {}, linkMD5: {}", link, linkMD5);
            }
        }
        return ServerResult.build(OperateStatus.SUCCESS);
    }

}
