package com.chaoqer.common.entity.base;

/**
 * 业务错误码
 */
public enum OperateErrorCode {

    // 重复请求
    DUPLICATE_CALL(-1, "短时间内重复调用"),

    // unknown
    UNKNOWN(0, "默认错误码"),

    // account
    ACCOUNT_NOT_FOUND(100000, "账号不存在"),
    ACCOUNT_EXISTED(100001, "账号已存在"),
    ACCOUNT_LOCKED(100002, "账号已被冻结"),

    // user
    USER_PROFILE_NOT_COMPLETE(200000, "用户资料未初始化"),
    USER_PROFILE_COMPLETE(200001, "用户资料已初始化"),
    USER_NOT_EXIST(200002, "用户不存在"),
    USER_MESSAGE_NOT_EXIST(200003, "消息不存在"),

    // app

    // room
    ROOM_NOT_FOUND(400000, "活动已解散"),
    ROOM_NOT_ALLOW_JOIN(400001, "无法加入此活动"),

    // club
    CLUB_NOT_FOUND(500000, "圈子不存在"),

    // event
    EVENT_NOT_FOUND(600000, "日程不存在");

    private final int code;
    private final String message;

    OperateErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static OperateErrorCode getOperateErrorCode(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (OperateErrorCode e : OperateErrorCode.values()) {
            if (e.code == code) {
                return e;
            }
        }
        return UNKNOWN;
    }

}
