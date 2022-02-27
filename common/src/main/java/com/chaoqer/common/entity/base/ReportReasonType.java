package com.chaoqer.common.entity.base;

/**
 * 投诉原因类型
 */
public enum ReportReasonType {

    ILLEGAL(1, "涉嫌违法违规"),
    PORN(2, "色情淫秽"),
    VIOLENCE(3, "暴力血腥"),
    JUNK_AD(4, "广告营销"),
    OTHER(5, "其它");

    private final int type;
    private final String reason;

    ReportReasonType(int type, String reason) {
        this.type = type;
        this.reason = reason;
    }

    public int getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    public static ReportReasonType getReportReasonType(Integer type) {
        if (type == null) {
            return null;
        }
        for (ReportReasonType e : ReportReasonType.values()) {
            if (e.type == type) {
                return e;
            }
        }
        return null;
    }

}
