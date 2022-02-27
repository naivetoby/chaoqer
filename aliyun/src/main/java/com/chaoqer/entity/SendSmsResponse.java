package com.chaoqer.entity;

import lombok.Data;

@Data
public class SendSmsResponse {

    private String Code;
    private String Message;
    private String RequestId;
    private String BizId;

}
