package com.chaoqer.common.entity.club;

import com.chaoqer.common.entity.base.AuthedDTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ClubIdDTO extends AuthedDTO {

    @NotBlank(message = "clubId不能为空")
    private String clubId;

}
