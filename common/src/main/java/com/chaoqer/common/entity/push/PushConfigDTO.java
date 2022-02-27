package com.chaoqer.common.entity.push;

import com.chaoqer.common.entity.base.AuthedDTO;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;

@Data
public class PushConfigDTO extends AuthedDTO {

    // 推送设置类型
    @NotNull(message = "pushConfigType类型不能为空")
    @Range(min = 0, max = 99, message = "pushConfigType类型不存在")
    private Integer pushConfigType;

    // 是否开启
    @NotNull(message = "enable类型不能为空")
    @Range(min = 0, max = 1, message = "enable类型不存在")
    private Integer enable;
}
