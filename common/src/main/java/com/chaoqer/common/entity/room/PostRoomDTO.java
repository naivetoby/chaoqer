package com.chaoqer.common.entity.room;

import com.chaoqer.common.entity.base.AuthedDTO;
import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class PostRoomDTO extends AuthedDTO {

    @Size(max = 100, message = "标题不能超过100个字")
    private String name;

    private String clubId;

    private int inviteOnly;

}
