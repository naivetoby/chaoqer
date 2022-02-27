package com.chaoqer.common.entity.push;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class AttachMessage {

    // 透传类型
    private Integer attachType;
    // 透传内容(可选)
    private JSONObject attachBody;

    public JSONObject getAttachBody() {
        if (attachBody == null) {
            return new JSONObject();
        }
        return attachBody;
    }

}
