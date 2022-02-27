package com.chaoqer.common.entity.event;

import com.chaoqer.common.entity.base.AuthedDTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class EventDTO extends AuthedDTO {

    private String eventId;

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题不能超过100个字")
    private String name;

    @Size(max = 500, message = "介绍不能超过500个字")
    private String desc;

    @NotNull(message = "日期时间不能为空")
    private Long eventTime;

    @Size(min = 1, message = "嘉宾或主持人不能为空")
    private List<String> memberUidList;

}
