package com.chaoqer.common.entity.user;

import com.chaoqer.common.entity.base.AuthedDTO;
import com.chaoqer.common.entity.user.group.InitProfileGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import javax.validation.groups.Default;

@Data
public class UserProfileDTO extends AuthedDTO {

    // 名字
    @NotBlank(message = "名字不能为空", groups = {Default.class, InitProfileGroup.class})
    @Size(max = 20, message = "设置的名字不能超过20个字", groups = {Default.class, InitProfileGroup.class})
    private String nickname;

    // 头像
    @NotBlank(message = "头像不能为空", groups = {Default.class, InitProfileGroup.class})
    private String avatar;

    // 介绍
    @Size(max = 1000, message = "介绍不能超过1000个字")
    private String bio;

}
