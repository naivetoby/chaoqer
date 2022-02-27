package com.chaoqer.common.entity.app;

import com.chaoqer.common.entity.base.AuthedDTO;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class FeedbackDTO extends AuthedDTO {

    // 反馈来源类型
    @NotNull(message = "反馈来源类型不能为空")
    @Range(min = 0, max = 1, message = "反馈来源类型不存在")
    private int originFeedbackType;

    // 反馈来源ID
    @NotNull(message = "反馈来源ID不能为空")
    private String originId;

    // 反馈类型
    @NotNull(message = "反馈类型不能为空")
    @Range(min = 0, max = 0, message = "反馈类型不存在")
    private int feedbackType;

    // 反馈内容
    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 500, message = "反馈内容不能超过500个字")
    private String feedbackContent;

    // 反馈图片
    private List<String> feedbackImageList;

}
