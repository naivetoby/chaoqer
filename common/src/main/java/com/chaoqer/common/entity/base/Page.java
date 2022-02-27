package com.chaoqer.common.entity.base;

import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;

@Data
public class Page {

    // 偏移量
    private String lastId;

    // 分页大小
    @NotNull(message = "count不能为空")
    @Range(min = 1, max = 100, message = "count只能在0到100之间")
    private int count;

}
