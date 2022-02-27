package com.chaoqer.common.entity.push;

/**
 * 消息类型
 */
public enum MessageType {

    // 透传消息
    ATTACH(-1, "attach"),
    // 通知消息
    ALERT(0, "alert");

    private final int type;
    private final String name;

    MessageType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static MessageType getMessageType(Integer type) {
        if (type == null) {
            return ALERT;
        }
        for (MessageType e : MessageType.values()) {
            if (e.type == type) {
                return e;
            }
        }
        return ALERT;
    }

}
