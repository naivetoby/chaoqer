package com.chaoqer.common.entity.base.filter;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ImageScanDTO extends OriginInputDTO {

    // 图片地址
    @NotBlank(message = "图片地址不能为空")
    private String imageUrl;

}
