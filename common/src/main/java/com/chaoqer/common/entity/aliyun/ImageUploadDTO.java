package com.chaoqer.common.entity.aliyun;

import com.chaoqer.common.entity.base.AuthedDTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ImageUploadDTO extends AuthedDTO {

    // 图片目录
    @NotBlank(message = "imageDir不能为空")
    private String imageDir;

}
