package com.chaoqer.common.entity.app;

import com.chaoqer.common.entity.base.AuthedDTO;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class ReportDTO extends AuthedDTO {

    // 投诉来源类型
    @NotNull(message = "投诉来源类型不能为空")
    @Range(min = 1, max = 2, message = "投诉来源类型不存在")
    private int originReportType;

    // 投诉来源ID
    @NotBlank(message = "投诉来源ID不能为空")
    private String originId;

    // 投诉原因类型
    @NotNull(message = "投诉原因类型不能为空")
    @Range(min = 1, max = 5, message = "投诉原因类型不存在")
    private int reportReasonType;

    // 投诉原因
    @NotBlank(message = "投诉原因不能为空")
    @Size(max = 500, message = "投诉原因不能超过500个字")
    private String reason;

}
