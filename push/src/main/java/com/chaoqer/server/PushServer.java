package com.chaoqer.server;

import cn.jiguang.common.ClientConfig;
import cn.jiguang.common.resp.APIConnectionException;
import cn.jiguang.common.resp.APIRequestException;
import cn.jpush.api.JPushClient;
import cn.jpush.api.device.DeviceClient;
import cn.jpush.api.push.PushResult;
import cn.jpush.api.push.model.Message;
import cn.jpush.api.push.model.Platform;
import cn.jpush.api.push.model.PushPayload;
import cn.jpush.api.push.model.audience.Audience;
import cn.jpush.api.push.model.notification.AndroidNotification;
import cn.jpush.api.push.model.notification.IosAlert;
import cn.jpush.api.push.model.notification.IosNotification;
import cn.jpush.api.push.model.notification.Notification;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chaoqer.client.PushAsyncClient;
import com.chaoqer.common.entity.push.*;
import com.chaoqer.common.entity.user.UidDTO;
import com.chaoqer.common.util.CommonUtil;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.entity.JPushProperties;
import com.chaoqer.repository.UserMessageOTS;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import vip.toby.rpc.annotation.RpcServer;
import vip.toby.rpc.annotation.RpcServerMethod;
import vip.toby.rpc.entity.OperateStatus;
import vip.toby.rpc.entity.RpcType;
import vip.toby.rpc.entity.ServerResult;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RpcServer(value = "push", type = RpcType.ASYNC)
public class PushServer {

    private final static Logger logger = LoggerFactory.getLogger(PushServer.class);

    @Autowired
    private Environment env;
    @Autowired
    private UserMessageOTS userMessageOTS;
    @Autowired
    private PushAsyncClient pushAsyncClient;
    @Autowired
    private JPushProperties jPushProperties;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @RpcServerMethod
    public ServerResult updateDevice(@Validated JPushIdDTO jPushIdDTO) {
        String uid = jPushIdDTO.getAuthedUid();
        String jPushId = jPushIdDTO.getJPushId();
        String jPushUpdateApiDisableKey = RedisKeyGenerator.getJPushUpdateApiDisableKey();
        try {
            String jPushUserDeviceIdKey = RedisKeyGenerator.getJPushUserDeviceIdKey(uid);
            if (!jPushId.equals(RedisUtil.getString(redisTemplate, jPushUserDeviceIdKey))) {
                // 先处理其他缓存
                RedisUtil.delObject(redisTemplate, jPushUserDeviceIdKey);
                // 删除上一个uid缓存
                String jPushDeviceIdUserKey = RedisKeyGenerator.getJPushDeviceIdUserKey(jPushId);
                String latestUid = RedisUtil.getString(redisTemplate, jPushDeviceIdUserKey);
                if (StringUtils.isNotBlank(latestUid)) {
                    RedisUtil.delObject(redisTemplate, RedisKeyGenerator.getJPushUserDeviceIdKey(latestUid));
                }
                // 是否暂时已禁用updateJPushDevice API
                if (RedisUtil.isKeyExist(redisTemplate, jPushUpdateApiDisableKey)) {
                    logger.warn("更新JPush设备API暂时被禁用, uid: {}, jPushId: {}", uid, jPushId);
                    return ServerResult.build(OperateStatus.SUCCESS);
                }
                // 根据不同的运行环境，给别名加上不同的前缀
                String alias = CommonUtil.getEnvironmentName(env).concat("_").concat(uid);
                DeviceClient deviceClient = new DeviceClient(jPushProperties.getMasterSecret(), jPushProperties.getAppKey());
                // 查询与此别名绑定的所有设备
                List<String> deviceIdList = deviceClient.getAliasDeviceList(alias, null).registration_ids;
                if (deviceIdList != null && deviceIdList.size() > 0) {
                    // 删除此别名与此别名绑定的所有设备
                    deviceClient.deleteAlias(alias, null);
                }
                // 将此设备绑定一个别名
                deviceClient.updateDeviceTagAlias(jPushId, alias, null, null);
                // 重新添加缓存
                RedisUtil.setObject(redisTemplate, jPushDeviceIdUserKey, uid, 3, TimeUnit.DAYS);
                // 如果在白名单中不缓存
                if (!jPushProperties.getWhiteUidList().contains(uid)) {
                    // 缓存3天
                    RedisUtil.setObject(redisTemplate, jPushUserDeviceIdKey, jPushId, 3, TimeUnit.DAYS);
                }
                logger.info("更新JPush设备成功, uid: {}, jPushId: {}", uid, jPushId);
            } else {
                // 已上传, 忽略
                logger.info("更新JPush设备已忽略, uid: {}, jPushId: {}", uid, jPushId);
            }
        } catch (APIConnectionException e) {
            logger.error("更新JPush设备异常, JPush服务器连接失败", e);
            // 锁定此API半小时
            RedisUtil.setObject(redisTemplate, jPushUpdateApiDisableKey, true, 60 * 30);
        } catch (Exception e) {
            logger.error("更新JPush设备异常, Exception: {}", e.getMessage());
        }
        return ServerResult.build(OperateStatus.SUCCESS);
    }

    @RpcServerMethod
    public ServerResult deleteDevice(@Validated UidDTO uidDTO) {
        String uid = uidDTO.getUid();
        String jPushUpdateApiDisableKey = RedisKeyGenerator.getJPushUpdateApiDisableKey();
        try {
            // 删除缓存
            RedisUtil.delObject(redisTemplate, RedisKeyGenerator.getJPushUserDeviceIdKey(uid));
            // 是否暂时已禁用updateJPushDevice API
            if (RedisUtil.isKeyExist(redisTemplate, jPushUpdateApiDisableKey)) {
                logger.warn("删除JPush设备API暂时被禁用, uid: {}", uid);
                return ServerResult.build(OperateStatus.SUCCESS);
            }
            DeviceClient deviceClient = new DeviceClient(jPushProperties.getMasterSecret(), jPushProperties.getAppKey());
            // 根据不同的运行环境，给别名加上不同的前缀
            String alias = CommonUtil.getEnvironmentName(env).concat("_").concat(uid);
            // 查询与此别名绑定的所有设备
            List<String> deviceIdList = deviceClient.getAliasDeviceList(alias, null).registration_ids;
            if (deviceIdList != null && deviceIdList.size() > 0) {
                // 删除此别名与此别名绑定的所有设备
                deviceClient.deleteAlias(alias, null);
            }
        } catch (APIConnectionException e) {
            logger.error("删除JPush设备异常, JPush服务器连接失败", e);
            // 锁定此API半小时
            RedisUtil.setObject(redisTemplate, jPushUpdateApiDisableKey, true, 60 * 30);
        } catch (Exception e) {
            logger.error("删除JPush设备异常, Exception: {}", e.getMessage());
        }
        return ServerResult.build(OperateStatus.SUCCESS);
    }

    @RpcServerMethod
    public ServerResult pushAlertMessage(@Validated AlertMessageDTO alertMessageDTO) throws InterruptedException {
        String uid = alertMessageDTO.getUid();
        OriginPushType originPushType = OriginPushType.getOriginPushType(alertMessageDTO.getOriginPushType());
        String originUid = "";
        if (originPushType == OriginPushType.INTERACTIVE) {
            originUid = alertMessageDTO.getOriginUid();
            if (StringUtils.isBlank(originUid)) {
                return ServerResult.buildFailureMessage("originUid不能为空");
            }
        }
        if (alertMessageDTO.getStore() == 0 && alertMessageDTO.getAlert() == 0) {
            return ServerResult.buildFailureMessage("store与alert不能同时为0");
        }
        if (alertMessageDTO.getStore() > 0) {
            // 异步存模板消息
            userMessageOTS.asyncSaveUserMessage(uid, originPushType, originUid, MessageType.ALERT, alertMessageDTO.getMessageBody());
            // 发送新消息提醒
            pushAsyncClient.pushAttachMessage(AttachMessageDTO.buildNewNotificationMessage(uid));
        }
        if (alertMessageDTO.getAlert() > 0) {
            try {
                ServerResult serverResult = doPush(originPushType, buildPushAlertMessage(CommonUtil.getEnvironmentName(env).concat("_").concat(uid), alertMessageDTO.getMessageBody()), alertMessageDTO);
                if (serverResult != null) {
                    return serverResult;
                }
            } catch (APIConnectionException e) {
                // 重试推送
                alertMessageDTO.setStore(0);
                pushAsyncClient.pushAlertMessage(alertMessageDTO);
                logger.error("APIConnectionException 推送失败, 已重新投入队列, Error Message: {}", e.getMessage());
                Thread.sleep(100);
            } catch (APIRequestException e) {
                // 处理用户已经退出客户端
                if (e.getErrorCode() == 1011) {
                    // 推送忽略
                    logger.warn("推送已忽略, Result: 用户已注销或者登录了非移动终端(如WEB), Message: {}", alertMessageDTO);
                } else {
                    logger.error("APIRequestException 推送失败, Error Message: {}", e.getErrorMessage());
                }
            } catch (Exception e) {
                logger.error("推送异常, Exception: " + e.getMessage(), e);
            }
        }
        return ServerResult.build(OperateStatus.SUCCESS);
    }

    @RpcServerMethod
    public ServerResult pushAttachMessage(@Validated AttachMessageDTO attachMessageDTO) throws InterruptedException {
        String uid = attachMessageDTO.getUid();
        OriginPushType originPushType = OriginPushType.getOriginPushType(attachMessageDTO.getOriginPushType());
        String originUid = "";
        if (originPushType == OriginPushType.INTERACTIVE) {
            originUid = attachMessageDTO.getOriginUid();
            if (StringUtils.isBlank(originUid)) {
                return ServerResult.buildFailureMessage("originUid不能为空");
            }
        }
        AttachType attachType = AttachType.getAttachType(attachMessageDTO.getAttachType());
        if (attachType == null) {
            return ServerResult.buildFailureMessage("attachType类型不存在");
        }
        if (attachMessageDTO.getStore() > 0) {
            // 异步存模板消息
            userMessageOTS.asyncSaveUserMessage(uid, originPushType, originUid, MessageType.ATTACH, attachMessageDTO.getMessageBody());
            // 发送新消息提醒
            pushAsyncClient.pushAttachMessage(AttachMessageDTO.buildNewNotificationMessage(uid));
        }
        AttachMessage attachMessage = new AttachMessage();
        attachMessage.setAttachType(attachType.getType());
        attachMessage.setAttachBody(attachMessageDTO.getAttachBody());
        try {
            ServerResult serverResult = doPush(originPushType, buildPushAttachMessage(CommonUtil.getEnvironmentName(env).concat("_").concat(uid), attachMessage), attachMessageDTO);
            if (serverResult != null) {
                return serverResult;
            }
        } catch (APIConnectionException e) {
            // 重试推送
            pushAsyncClient.pushAttachMessage(attachMessageDTO);
            logger.error("APIConnectionException 推送失败, 已重新投入队列, Error Message: {}", e.getMessage());
            Thread.sleep(100);
        } catch (APIRequestException e) {
            // 处理用户已经退出客户端
            if (e.getErrorCode() == 1011) {
                // 推送忽略
                logger.warn("推送已忽略, Result: 用户已注销或者登录了非移动终端(如WEB), Message: {}", attachMessageDTO);
            } else {
                logger.error("APIRequestException 推送失败, Error Message: {}", e.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("推送异常, Exception: " + e.getMessage(), e);
        }
        return ServerResult.build(OperateStatus.SUCCESS);
    }

    private JPushClient buildJPushClient(boolean isProd, OriginPushType originPushType) {
        long timeToLive = jPushProperties.getTimeToLive().getOfficial();
        if (originPushType == OriginPushType.INTERACTIVE) {
            timeToLive = jPushProperties.getTimeToLive().getInteractive();
        } else if (originPushType == OriginPushType.SYSTEM) {
            timeToLive = jPushProperties.getTimeToLive().getSystem();
        }
        // 配置文件
        ClientConfig clientConfig = ClientConfig.getInstance();
        clientConfig.setMaxRetryTimes(3);
        clientConfig.setConnectionTimeout(5 * 1000);
        clientConfig.setConnectionRequestTimeout(5 * 1000);
        clientConfig.setSocketTimeout(5 * 1000);
        clientConfig.setGlobalPushSetting(isProd, timeToLive);
        // 连接推送服务器
        return new JPushClient(jPushProperties.getMasterSecret(), jPushProperties.getAppKey(), null, clientConfig);
    }

    private ServerResult doPush(OriginPushType originPushType, PushPayload pushPayload, PushMessageDTO pushMessageDTO) throws APIConnectionException, APIRequestException, InterruptedException {
        boolean isProd = CommonUtil.isDevEnvironment(env);
        JPushClient jPushClient = buildJPushClient(isProd, originPushType);
        PushResult result = jPushClient.sendPush(pushPayload);
        if (!result.isResultOK()) {
            logger.error("推送失败, Error Message: {}, statusCode: {}", result.getOriginalContent(), result.statusCode);
            return ServerResult.build(OperateStatus.SUCCESS);
        }
        logger.info("推送成功, Result: {}, Message: {}", result, pushMessageDTO);
        Thread.sleep(100);
        jPushClient.close();
        // 是否白名单之内的用户，再推送一遍
        if (isProd && jPushProperties.getWhiteUidList().contains(pushMessageDTO.getUid())) {
            jPushClient = buildJPushClient(false, originPushType);
            result = jPushClient.sendPush(pushPayload);
            if (!result.isResultOK()) {
                logger.error("推送失败, Error Message: {}, statusCode: {}", result.getOriginalContent(), result.statusCode);
                return ServerResult.build(OperateStatus.SUCCESS);
            }
            logger.info("推送成功, Result: {}, Message: {}", result, pushMessageDTO);
            Thread.sleep(100);
            jPushClient.close();
        }
        return null;
    }

    // 通知消息
    private PushPayload buildPushAlertMessage(String alias, JSONObject messageBody) {
        return PushPayload.newBuilder()
                .setPlatform(Platform.android_ios())
                .setAudience(Audience.alias(alias))
                .setNotification(Notification.newBuilder()
                        .addPlatformNotification(IosNotification.newBuilder()
                                .setAlert(IosAlert.newBuilder().setTitleAndBody("", messageBody.getString("title"), messageBody.getString("content")).build())
                                .setSound("default")
                                .setContentAvailable(true)
                                .setMutableContent(true)
                                .incrBadge(1)
                                .addExtra("url", messageBody.getString("url"))
                                .build())
                        .addPlatformNotification(AndroidNotification.newBuilder()
                                .setTitle(messageBody.getString("title"))
                                .setAlert(messageBody.getString("content"))
                                .setStyle(1)
                                .setBigText(messageBody.getString("content"))
                                .addExtra("url", messageBody.getString("url"))
                                .build())
                        .build())
                .build();
    }

    // 透传消息
    private PushPayload buildPushAttachMessage(String alias, AttachMessage attachMessage) {
        return PushPayload.newBuilder()
                .setPlatform(Platform.android_ios())
                .setAudience(Audience.alias(alias))
                .setNotification(Notification.newBuilder()
                        .addPlatformNotification(IosNotification.newBuilder()
                                .setAlert("")
                                .incrBadge(0)
                                .build())
                        .addPlatformNotification(AndroidNotification.newBuilder()
                                .setAlert("")
                                .build())
                        .build())
                .setMessage(Message.newBuilder()
                        .setMsgContent(JSON.toJSONString(attachMessage))
                        .build())
                .build();
    }

}
