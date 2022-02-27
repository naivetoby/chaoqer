package com.chaoqer.common.entity.base;

/**
 * 性别类型
 */
public enum GenderType {

    // 女
    FEMALE(0, "female"),
    // 男
    MALE(1, "male");

    private final int value;
    private final String name;

    GenderType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static GenderType getGenderType(Integer value) {
        if (value == null) {
            return null;
        }
        for (GenderType e : GenderType.values()) {
            if (e.value == value) {
                return e;
            }
        }
        return null;
    }

}
