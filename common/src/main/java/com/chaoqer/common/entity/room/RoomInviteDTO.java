package com.chaoqer.common.entity.room;

import com.chaoqer.common.entity.base.AuthedDTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RoomInviteDTO extends AuthedDTO {

    @NotBlank(message = "roomId不能为空")
    private String roomId;

    @NotBlank(message = "uid不能为空")
    private String uid;

}
