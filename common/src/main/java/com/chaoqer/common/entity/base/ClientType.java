package com.chaoqer.common.entity.base;

/**
 * 客户端类型
 */
public enum ClientType {

    // 网页
    WEB(0, "Web"),
    // iPhone
    IOS(1, "iOS"),
    // 安卓
    ANDROID(2, "Android");

    private final int type;
    private final String name;

    ClientType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static ClientType getClientType(Integer type) {
        if (type == null) {
            return WEB;
        }
        for (ClientType e : ClientType.values()) {
            if (e.type == type) {
                return e;
            }
        }
        return WEB;
    }

}
