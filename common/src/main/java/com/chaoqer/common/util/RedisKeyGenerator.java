package com.chaoqer.common.util;

/**
 * redis的key值管理器
 *
 * @author toby
 */
public class RedisKeyGenerator {

    /**
     * Simple-Rpc 调用缓存 消息ID
     */
    public static String getSimpleRpcCorrelationIdKey(String correlationId) {
        return "SIMPLE_RPC_CORRELATION_ID_".concat(correlationId);
    }

    /**
     * Simple-Rpc 调用缓存 详细信息MD5特征值
     */
    public static String getSimpleRpcDetailMd5Key(String md5) {
        return "SIMPLE_RPC_DETAIL_MD5_".concat(md5);
    }

    /**
     * JPush-Uid-DeviceId
     */
    public static String getJPushUserDeviceIdKey(String uid) {
        return "LS_JPUSH_UID_DEVICE_ID_".concat(uid);
    }

    /**
     * JPush-DeviceId-Uid
     */
    public static String getJPushDeviceIdUserKey(String deviceId) {
        return "LS_JPUSH_DEVICE_ID_UID_".concat(deviceId);
    }

    /**
     * JPush-Update-Api-Disable
     */
    public static String getJPushUpdateApiDisableKey() {
        return "LS_JPUSH_UPDATE_API_DISABLE";
    }

    /**
     * 微信参数
     */
    public static String getWXAccessTokenKey() {
        return "WX_ACCESS_TOKEN";
    }

    /**
     * 微信参数
     */
    public static String getWXJsApiTicketKey() {
        return "WX_JS_API_TICKET";
    }

    /**
     * 登录相关的openID
     */
    public static String getLoginUserOpenIdKey(String openId) {
        return "GK_AUTH_LOGIN_USER_OPENID_".concat(openId);
    }

    /**
     * openID的缓存(通过code)
     */
    public static String getOpenIdCodeKey(String code) {
        return "GK_WX_USER_OPENID_CODE_".concat(code);
    }

    /**
     * openID的缓存(通过uid)
     */
    public static String getOpenIdUidKey(String uid) {
        return "GK_WX_USER_OPENID_UID_".concat(uid);
    }

    /**
     * 登录相关的cookie
     */
    public static String getLoginUserCookieKey(String cookieValue) {
        return "GK_AUTH_LOGIN_USER_COOKIE_".concat(cookieValue);
    }

    /**
     * OSS Upload Role STS 缓存
     */
    public static String getOssUploadRoleStsKey() {
        return "CQ_OSS_UPLOAD_ROLE_STS";
    }

    /**
     * OSS List Role STS 缓存
     */
    public static String getOssListRoleStsKey() {
        return "CQ_OSS_LIST_ROLE_STS";
    }

    /**
     * 记录账户更新过期时间缓存
     */
    public static String getAccountUpdateExpireTimeKey(String uid) {
        return "CQ_ACCOUNT_UPDATE_EXPIRE_TIME_".concat(uid);
    }

    /**
     * 记录账户更新上次登录时间缓存
     */
    public static String getAccountUpdateActiveTimeKey(String uid) {
        return "CQ_ACCOUNT_UPDATE_ACTIVE_TIME_".concat(uid);
    }

    /**
     * 记录账户客户端 详细信息MD5特征值
     */
    public static String getAccountClientMetaMd5Key(String md5) {
        return "CQ_ACCOUNT_CLIENT_META_MD5_".concat(md5);
    }

    /**
     * 账户登录token缓存
     */
    public static String getAccountTokenKey(String uid) {
        return "CQ_ACCOUNT_TOKEN_".concat(uid);
    }

    /**
     * 账户登录secret缓存
     */
    public static String getAccountSecretKey(String uid) {
        return "CQ_ACCOUNT_SECRET_".concat(uid);
    }

    /**
     * 账户登录status缓存
     */
    public static String getAccountStatusKey(String uid) {
        return "CQ_ACCOUNT_STATUS_".concat(uid);
    }

    /**
     * 账户短信验证码缓存
     */
    public static String getAccountCaptchaKey(String countryCode, String mobile) {
        return "CQ_ACCOUNT_V_CODE_".concat(countryCode).concat("_").concat(mobile);
    }

    /**
     * 账户验证token缓存
     */
    public static String getAccountVerifyTokenKey(String uid) {
        return "CQ_ACCOUNT_VERIFY_TOKEN_".concat(uid);
    }

    /**
     * 账户短信验证码间隔缓存
     */
    public static String getAccountCaptchaSendKey(String countryCode, String mobile) {
        return "CQ_ACCOUNT_V_CODE_SEND_".concat(countryCode).concat("_").concat(mobile);
    }

    /**
     * 记录账户验证码验证次数
     */
    public static String getAccountCaptchaVerifyCountKey(String countryCode, String mobile) {
        return "CQ_ACCOUNT_V_CODE_VERIFY_".concat(countryCode).concat("_").concat(mobile);
    }

    /**
     * 用户资料缓存
     */
    public static String getUserProfileKey(String uid) {
        return "CQ_USER_PROFILE_".concat(uid);
    }

    /**
     * 拉黑缓存
     */
    public static String getUserBlockKey(String uid, String blockUid) {
        return "CQ_USER_BLOCK_".concat(uid).concat("_").concat(blockUid);
    }

    /**
     * 圈子缓存
     */
    public static String getClubKey(String clubId) {
        return "CQ_CLUB_".concat(clubId);
    }

    /**
     * 圈子人数缓存
     */
    public static String getClubMemberTotalKey(String clubId) {
        return "CQ_CLUB_MEMBER_TOTAL_".concat(clubId);
    }

    /**
     * 活动缓存
     */
    public static String getRoomKey(String roomId) {
        return "CQ_ROOM_".concat(roomId);
    }

    /**
     * 活动Cache缓存
     */
    public static String getRoomCacheKey(String roomCacheId) {
        return "CQ_ROOM_CACHE_".concat(roomCacheId);
    }

    /**
     * 活动参与成员缓存
     */
    public static String getRoomMemberListKey(String roomId) {
        return "CQ_ROOM_MEMBER_LIST_".concat(roomId);
    }

    /**
     * 活动参与成员Icon缓存
     */
    public static String getRoomMemberIconKey(String roomId, String uid) {
        return "CQ_ROOM_MEMBER_ICON_".concat(roomId).concat("_").concat(uid);
    }

    /**
     * 活动参与成员身份保持缓存
     */
    public static String getRoomMemberRoleKey(String roomId, String uid) {
        return "CQ_ROOM_MEMBER_ROLE_".concat(roomId).concat("_").concat(uid);
    }

    /**
     * 活动参与成员Icon Timeout缓存
     */
    public static String getRoomMemberIconTimeoutKey(String roomId, String uid) {
        return "CQ_ROOM_MEMBER_ICON_TIMEOUT_".concat(roomId).concat("_").concat(uid);
    }

    /**
     * 活动的pin信息
     */
    public static String getRoomPinDataKey(String roomId) {
        return "CQ_ROOM_PIN_DATA_".concat(roomId);
    }

    /**
     * 活动的live信息
     */
    public static String getRoomLiveDataKey(String roomId) {
        return "CQ_ROOM_LIVE_DATA_".concat(roomId);
    }

    /**
     * 活动的request信息
     */
    public static String getRoomRequestDataKey(String roomId) {
        return "CQ_ROOM_REQUEST_DATA_".concat(roomId);
    }

    /**
     * 活动的response信息
     */
    public static String getRoomResponseDataKey(String roomId) {
        return "CQ_ROOM_RESPONSE_DATA_".concat(roomId);
    }

    /**
     * 活动的ruid去重
     */
    public static String getRoomRuidKey(String roomId, long ruid) {
        return "CQ_ROOM_RUID_".concat(roomId).concat("_") + ruid;
    }

    /**
     * 活动声网Token缓存
     */
    public static String getRoomAgoraTokenKey(String roomId) {
        return "CQ_ROOM_AGORA_TOKEN_".concat(roomId);
    }

    /**
     * 用户消息缓存
     */
    public static String getUserMessageKey(long messageId) {
        return "CQ_USER_MESSAGE_" + messageId;
    }

    /**
     * 用户未读消息总数缓存
     */
    public static String getUserMessageUnReadTotalKey(String uid) {
        return "CQ_USER_MESSAGE_UNREAD_TOTAL_".concat(uid);
    }

    /**
     * 用户是否发送过名片缓存
     */
    public static String getUserSendCardKey(String originUid, String uid) {
        return "CQ_USER_SEND_CARD_".concat(originUid).concat("_").concat(uid);
    }

    /**
     * 用户名片夹总数
     */
    public static String getUserCardTotalKey(String uid) {
        return "CQ_USER_CARD_TOTAL_".concat(uid);
    }

    /**
     * 版本信息缓存
     */
    public static String getAppVersionKey(String versionId) {
        return "CQ_APP_VERSION_".concat(versionId);
    }

    /**
     * 最新版本缓存
     */
    public static String getAppLatestVersionKey(int clientType, int versionCode) {
        return "CQ_APP_LATEST_VERSION_" + clientType + "_" + versionCode;
    }

    /**
     * 链接预览信息
     */
    public static String getAppLinkPreviewKey(String linkMD5) {
        return "CQ_APP_LINK_PREVIEW_".concat(linkMD5);
    }

    /**
     * 日程缓存
     */
    public static String getEventKey(String eventId) {
        return "CQ_EVENT_".concat(eventId);
    }

    /**
     * 日程预约缓存
     */
    public static String getEventNotifyKey(String eventId, String uid) {
        return "CQ_EVENT_NOTIFY_".concat(eventId).concat("_").concat(uid);
    }

}
