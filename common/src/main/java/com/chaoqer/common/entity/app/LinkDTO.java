package com.chaoqer.common.entity.app;

import com.chaoqer.common.entity.base.AuthedDTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LinkDTO extends AuthedDTO {

    // 网址
    @NotBlank(message = "网址不能为空")
    private String link;

}
