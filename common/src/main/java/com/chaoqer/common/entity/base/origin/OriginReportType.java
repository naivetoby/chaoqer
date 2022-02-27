package com.chaoqer.common.entity.base.origin;

/**
 * 投诉来源类型
 */
public enum OriginReportType {

    // 用户
    USER(1, "user"),
    // 活动
    ROOM(2, "room");

    private final int type;
    private final String name;

    OriginReportType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static OriginReportType getOriginReportType(Integer type) {
        if (type == null) {
            return null;
        }
        for (OriginReportType e : OriginReportType.values()) {
            if (e.type == type) {
                return e;
            }
        }
        return null;
    }

}
