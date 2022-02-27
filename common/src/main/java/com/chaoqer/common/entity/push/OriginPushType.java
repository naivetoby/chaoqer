package com.chaoqer.common.entity.push;

/**
 * 推送来源类型
 */
public enum OriginPushType {

    // 系统
    SYSTEM(0, "system"),
    // 官方
    OFFICIAL(1, "official"),
    // 互动
    INTERACTIVE(2, "interactive");

    private final int type;
    private final String name;

    OriginPushType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static OriginPushType getOriginPushType(Integer type) {
        if (type == null) {
            return SYSTEM;
        }
        for (OriginPushType e : OriginPushType.values()) {
            if (e.type == type) {
                return e;
            }
        }
        return SYSTEM;
    }
}
