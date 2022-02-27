package com.chaoqer.common.entity.base;

/**
 * 审核状态
 */
public enum AuditStatus {

    // 未审核(静默)
    DEFAULT(0, "default"),
    // 需要平台审核
    NEED_PLATFORM_AUDIT(1, "needPlatformAudit"),
    // 审核通过
    PASSED(2, "passed"),
    // 第三方审核不通过
    UN_PASSED_BY_THIRD_PARTY(-1, "unPassedByThirdParty"),
    // 平台审核不通过
    UN_PASSED_BY_PLATFORM(-2, "unPassedByPlatform");

    private final int status;
    private final String name;

    AuditStatus(int status, String name) {
        this.status = status;
        this.name = name;
    }

    public int getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public static AuditStatus getAuditStatus(Integer status) {
        if (status == null) {
            return null;
        }
        for (AuditStatus e : AuditStatus.values()) {
            if (e.status == status) {
                return e;
            }
        }
        return null;
    }

}
