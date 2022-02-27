package com.chaoqer.common.entity.base;

/**
 * 删除状态
 */
public enum DeleteStatus {

    // 正常
    NORMAL(0, "normal"),
    // 用户删除
    DELETED_BY_USER(1, "deletedByUser"),
    // 平台删除
    DELETED_BY_PLATFORM(2, "deletedByPlatform");

    private final int status;
    private final String name;

    DeleteStatus(int status, String name) {
        this.status = status;
        this.name = name;
    }

    public int getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public static DeleteStatus getDeleteStatus(Integer status) {
        if (status == null) {
            return null;
        }
        for (DeleteStatus e : DeleteStatus.values()) {
            if (e.status == status) {
                return e;
            }
        }
        return null;
    }

}
