package com.chaoqer.common.entity.club;

import com.chaoqer.common.entity.base.AuthedDTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class PostClubDTO extends AuthedDTO {

    @NotBlank(message = "名称不能为空")
    @Size(max = 100, message = "名称不能超过100个字")
    private String name;

    @NotBlank(message = "封面不能为空")
    private String cover;

}
