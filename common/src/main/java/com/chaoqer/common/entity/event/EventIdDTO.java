package com.chaoqer.common.entity.event;

import com.chaoqer.common.entity.base.AuthedDTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class EventIdDTO extends AuthedDTO {

    @NotBlank(message = "eventId不能为空")
    private String eventId;

}
