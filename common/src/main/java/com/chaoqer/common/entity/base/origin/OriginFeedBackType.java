package com.chaoqer.common.entity.base.origin;

/**
 * 建议与反馈来源类型
 */
public enum OriginFeedBackType {

    // App
    APP(0, "app"),
    // Room
    ROOM(1, "room");

    private final int type;
    private final String name;

    OriginFeedBackType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static OriginFeedBackType getOriginFeedBackType(Integer type) {
        if (type == null) {
            return APP;
        }
        for (OriginFeedBackType e : OriginFeedBackType.values()) {
            if (e.type == type) {
                return e;
            }
        }
        return APP;
    }

}
