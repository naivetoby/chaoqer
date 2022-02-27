package com.chaoqer.common.entity.base;

/**
 * 分值类型
 */
public enum ScoreType {

    // 开启
    ENABLE(1, "enable"),
    // 取消
    DISABLE(0, "disable");

    private final int score;
    private final String name;

    ScoreType(int score, String name) {
        this.score = score;
        this.name = name;
    }

    public int getScore() {
        return score;
    }

    public String getName() {
        return name;
    }

    public static ScoreType getScoreType(Integer score) {
        if (score == null) {
            return null;
        }
        for (ScoreType e : ScoreType.values()) {
            if (e.score == score) {
                return e;
            }
        }
        return null;
    }

}
