package com.chaoqer.common.entity.base;

/**
 * 登录类型
 */
public enum LoginType {

    // 短信验证码
    SMS(0, "sms"),
    // 密码
    PASSWORD(1, "password"),
    // 微信
    WECHAT(2, "wechat"),
    // 苹果
    APPLE(3, "apple"),
    // QQ
    QQ(4, "qq");

    private final int type;
    private final String name;

    LoginType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static LoginType getLoginType(Integer type) {
        if (type == null) {
            return SMS;
        }
        for (LoginType e : LoginType.values()) {
            if (e.type == type) {
                return e;
            }
        }
        return SMS;
    }

}
