package com.chaoqer.common.entity.base.filter;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class OriginInputDTO {

    // 输入来源用户
    @NotBlank(message = "originInputUid不能为空")
    private String originInputUid;

    // 输入来源类型
    @NotNull(message = "originInputType不能为空")
    private Integer originInputType;

}
