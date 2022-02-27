package com.chaoqer.common.entity.base;

/**
 * 用户状态
 */
public enum AccountStatus {

    // 已锁定
    DELETED(-99, "已注销"),
    // 已冻结
    LOCKED(-2, "已冻结"),
    // 已限制
    LIMITED(-1, "已限制"),
    // 注册中
    DEFAULT(0, "注册中"),
    // 正常
    NORMAL(1, "正常");

    private final int status;
    private final String name;

    AccountStatus(int status, String name) {
        this.status = status;
        this.name = name;
    }

    public int getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public static AccountStatus getAccountStatus(Integer status) {
        if (status == null) {
            return null;
        }
        for (AccountStatus e : AccountStatus.values()) {
            if (e.status == status) {
                return e;
            }
        }
        return null;
    }

}
