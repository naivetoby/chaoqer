package com.chaoqer.common.entity.club;

import com.chaoqer.common.entity.base.PageResult;
import lombok.Data;

@Data
public class ClubPageResult extends PageResult {

    // Club ID
    private String clubId;
    // Club
    private ClubResult clubResult;
    // 创建时间
    private long createTime;

}
