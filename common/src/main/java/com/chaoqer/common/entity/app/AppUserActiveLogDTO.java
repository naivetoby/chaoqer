package com.chaoqer.common.entity.app;

import com.chaoqer.common.entity.base.AuthedDTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
public class AppUserActiveLogDTO extends AuthedDTO {

    // API 路径
    @NotBlank(message = "apiPath不能为空")
    private String apiPath;

    // 参数
    @NotNull(message = "parameterMap不能为空")
    private Map<String, String[]> parameterMap;

    // 客户端公网IP
    @NotBlank(message = "ip不能为空")
    private String ip;

}
