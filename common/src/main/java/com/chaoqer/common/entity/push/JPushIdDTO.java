package com.chaoqer.common.entity.push;

import com.chaoqer.common.entity.base.AuthedDTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class JPushIdDTO extends AuthedDTO {

    @NotBlank(message = "jPushId不能为空")
    private String jPushId;

}
