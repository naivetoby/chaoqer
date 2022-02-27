package com.chaoqer.common.entity.base.filter;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class TextScanDTO extends OriginInputDTO {

    // 文本内容
    @NotBlank(message = "文本内容不能为空")
    private String textContext;

}
