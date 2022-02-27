package com.chaoqer.common.entity.push;

import com.chaoqer.common.entity.base.PageDTO;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;

@Data
public class UserMessagePageDTO extends PageDTO {

    // 来源
    @NotNull(message = "originPushType类型不能为空")
    @Range(min = 0, max = 2, message = "originPushType类型不存在")
    private Integer originPushType;

}