package com.chaoqer.common.entity.user;

import com.chaoqer.common.entity.base.AuthedDTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UidDTO extends AuthedDTO {

    @NotBlank(message = "uid不能为空")
    private String uid;

}
