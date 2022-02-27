package com.chaoqer.common.entity.push;

/**
 * 透传消息类型
 */
public enum AttachType {

    // 对话框
    DIALOG(1, "dialog"),
    // 退出登录
    LOGOUT(2, "logout"),
    // 新提醒
    NEW_NOTIFICATION(3, "new_notification");

    private final int type;
    private final String name;

    AttachType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static AttachType getAttachType(Integer type) {
        if (type == null) {
            return null;
        }
        for (AttachType e : AttachType.values()) {
            if (e.type == type) {
                return e;
            }
        }
        return null;
    }

}
