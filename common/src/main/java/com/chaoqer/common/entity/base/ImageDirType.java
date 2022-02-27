package com.chaoqer.common.entity.base;

import org.apache.commons.lang3.StringUtils;

/**
 * 图片上传目录类型
 */
public enum ImageDirType {

    // 封面图
    COVER("cover"),
    // 头像
    AVATAR("avatar"),
    // 活动
    ROOM("room"),
    // 反馈
    FEEDBACK("feedback");

    private final String name;

    ImageDirType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ImageDirType getName(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        for (ImageDirType e : ImageDirType.values()) {
            if (e.name.equals(name)) {
                return e;
            }
        }
        return null;
    }

}
