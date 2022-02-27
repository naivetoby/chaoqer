package com.chaoqer.common.entity.aliyun;

import com.chaoqer.common.entity.base.AuthedDTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class FileDirDTO extends AuthedDTO {

    // 文件目录
    @NotBlank(message = "fileDir不能为空")
    private String fileDir;

}
