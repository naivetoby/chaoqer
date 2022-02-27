package com.chaoqer.common.entity.base.origin;

/**
 * 输入来源类型
 */
public enum OriginInputType {

    // 用户名字
    USER_NICKNAME(InputType.TEXT, 1, "nickname", "名字"),
    // 用户介绍
    USER_BIO(InputType.TEXT, 2, "bio", "介绍"),
    // 用户头像
    USER_AVATAR(InputType.IMAGE, 3, "avatar", "头像"),
    // 活动标题
    ROOM_NAME(InputType.TEXT, 4, "name", "标题"),
    // 圈子名称
    CLUB_NAME(InputType.TEXT, 5, "name", "名称"),
    // 圈子照片
    CLUB_PHOTO(InputType.IMAGE, 6, "photo", "照片");


    private final InputType inputType;
    private final int type;
    private final String column;
    private final String name;

    OriginInputType(InputType inputType, int type, String column, String name) {
        this.inputType = inputType;
        this.type = type;
        this.column = column;
        this.name = name;
    }

    public InputType getInputType() {
        return inputType;
    }

    public int getType() {
        return type;
    }

    public String getColumn() {
        return column;
    }

    public String getName() {
        return name;
    }

    public static OriginInputType getOriginInputType(Integer type) {
        if (type == null) {
            return null;
        }
        for (OriginInputType e : OriginInputType.values()) {
            if (e.type == type) {
                return e;
            }
        }
        return null;
    }

    public enum InputType {
        TEXT, IMAGE;
    }

}
