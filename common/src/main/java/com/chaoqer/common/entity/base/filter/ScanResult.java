package com.chaoqer.common.entity.base.filter;

import lombok.Data;

@Data
public class ScanResult {

    // 检测场景
    private String scene;

    /**
     * 建议您执行的后续操作。取值：
     * pass：文本正常，可以直接放行。
     * review：文本需要进一步人工审核。
     * block：文本违规，可以直接删除或者限制公开
     */
    // 建议后续操作
    private String suggestion;

    /**
     * 文本垃圾检测结果的分类。取值：
     * normal：正常文本
     * spam：含垃圾信息
     * ad：广告
     * politics：涉政
     * terrorism：暴恐
     * abuse：辱骂
     * porn：色情
     * flood：灌水
     * contraband：违禁
     * meaningless：无意义
     * customized：自定义（例如命中自定义关键词）
     */
    // 检测结果分类
    private String label;

    /**
     * 置信度分数，取值范围：0（表示置信度最低）~100（表示置信度最高）。
     * 如果suggestion为pass，则置信度越高，表示内容正常的可能性越高；如果suggestion为review或block，则置信度越高，表示内容违规的可能性越高。
     */
    private Float rate;

}