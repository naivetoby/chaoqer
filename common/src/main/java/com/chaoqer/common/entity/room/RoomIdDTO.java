package com.chaoqer.common.entity.room;

import com.chaoqer.common.entity.base.AuthedDTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RoomIdDTO extends AuthedDTO {

    @NotBlank(message = "roomId不能为空")
    private String roomId;

}
