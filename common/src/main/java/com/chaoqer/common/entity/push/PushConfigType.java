package com.chaoqer.common.entity.push;

/**
 * 推送设置类型
 */
public enum PushConfigType {

    // 所有通知
    ALL(99, "all", 1),
    // 免打扰
    DO_NOT_DISTURB(98, "doNotDisturb", 0),
    // 系统
    SYSTEM(0, "system", 1),
    // 官方
    OFFICIAL(1, "official", 1),
    // 互动
    INTERACTIVE(2, "interactive", 1);

    private final int type;
    private final String name;
    private final int defaultValue;

    PushConfigType(int type, String name, int defaultValue) {
        this.type = type;
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public int getDefaultValue() {
        return defaultValue;
    }

    public static PushConfigType getPushConfigType(Integer type) {
        if (type == null) {
            return ALL;
        }
        for (PushConfigType e : PushConfigType.values()) {
            if (e.type == type) {
                return e;
            }
        }
        return ALL;
    }

}
