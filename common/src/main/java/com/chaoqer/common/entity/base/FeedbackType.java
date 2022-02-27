package com.chaoqer.common.entity.base;

/**
 * 反馈类型
 */
public enum FeedbackType {

    DEFAULT(0, "默认");
    // 建议
    // 功能bug
    // UI异常
    // 内容太烂

    private final int type;
    private final String name;

    FeedbackType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static FeedbackType getFeedbackType(Integer type) {
        if (type == null) {
            return DEFAULT;
        }
        for (FeedbackType e : FeedbackType.values()) {
            if (e.type == type) {
                return e;
            }
        }
        return DEFAULT;
    }

}
