package com.chaoqer.server;

import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.chaoqer.common.entity.aliyun.AccessSmsDTO;
import com.chaoqer.entity.AppSmsLog;
import com.chaoqer.entity.SendSmsResponse;
import com.chaoqer.repository.AccountOTS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import vip.toby.rpc.annotation.RpcServer;
import vip.toby.rpc.annotation.RpcServerMethod;
import vip.toby.rpc.entity.OperateStatus;
import vip.toby.rpc.entity.RpcType;
import vip.toby.rpc.entity.ServerResult;

@RpcServer(value = "aliyun-sms", type = RpcType.ASYNC, threadNum = 4)
public class AliyunSmsAsyncServer {

    private final static Logger logger = LoggerFactory.getLogger(AliyunSmsAsyncServer.class);

    @Value("${aliyun.sms.sign-name}")
    private String signName;
    @Value("${aliyun.sms.access-template-code}")
    private String accessTemplateCode;

    @Autowired
    private AccountOTS accountOTS;
    @Autowired
    private IAcsClient smsClient;

    // 发送验证短信
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult createAccessSms(@Validated AccessSmsDTO accessSmsDTO) {
        AppSmsLog appSmsLog = new AppSmsLog();
        // 发送短信
        try {
            CommonRequest request = new CommonRequest();
            request.setSysMethod(MethodType.POST);
            request.setSysDomain("dysmsapi.aliyuncs.com");
            request.setSysVersion("2017-05-25");
            request.setSysAction("SendSms");
            request.putQueryParameter("RegionId", "cn-hangzhou");
            request.putQueryParameter("PhoneNumbers", accessSmsDTO.getMobile());
            request.putQueryParameter("SignName", signName);
            request.putQueryParameter("TemplateCode", accessTemplateCode);
            request.putQueryParameter("TemplateParam", "{\"code\":\"" + accessSmsDTO.getCaptcha() + "\"}");
            appSmsLog.setContent(accessSmsDTO.getCaptcha());
            appSmsLog.setReqData("signName: " + signName + ", templateCode: " + accessTemplateCode);
            // 发送短信
            CommonResponse commonResponse = smsClient.getCommonResponse(request);
            if (commonResponse.getHttpStatus() == 200) {
                SendSmsResponse sendSmsResponse = JSONObject.parseObject(commonResponse.getData(), SendSmsResponse.class);
                if (sendSmsResponse.getCode() != null && sendSmsResponse.getCode().equals("OK")) {
                    logger.info("短信发送成功. 手机号: " + accessSmsDTO.getMobile() + " | 验证码: " + accessSmsDTO.getCaptcha() + " | 请求返回值BizId: " + sendSmsResponse.getBizId());
                    appSmsLog.setResData("成功, BizId:" + sendSmsResponse.getBizId());
                } else {
                    logger.error("短信发送失败. 手机号: " + accessSmsDTO.getMobile() + " | 验证码: " + accessSmsDTO.getCaptcha() + " | 错误, RequestId: " + sendSmsResponse.getRequestId() + ", ErrorCode: " + sendSmsResponse.getCode() + ", Message: " + sendSmsResponse.getMessage());
                    appSmsLog.setResData("错误, RequestId: " + sendSmsResponse.getRequestId() + ", ErrorCode: " + sendSmsResponse.getCode() + ", Message: " + sendSmsResponse.getMessage());
                }
            } else {
                logger.error("短信发送失败. 手机号: " + accessSmsDTO.getMobile() + " | 验证码: " + accessSmsDTO.getCaptcha() + " | 错误原因: 网络异常 | HttpStatus: " + commonResponse.getHttpStatus());
                appSmsLog.setResData("错误, 网络异常 | HttpStatus: " + commonResponse.getHttpStatus());
            }
        } catch (ClientException se) {
            logger.error("短信发送失败. 手机号: " + accessSmsDTO.getMobile() + " | 验证码: " + accessSmsDTO.getCaptcha() + " | 错误, RequestId: " + se.getRequestId() + ", ErrorCode: " + se.getErrCode() + ", Message: " + se.getErrMsg());
            appSmsLog.setResData("错误, RequestId: " + se.getRequestId() + ", ErrorCode: " + se.getErrCode() + ", Message: " + se.getErrMsg());
        } catch (Exception e) {
            logger.error("短信发送失败. 手机号: " + accessSmsDTO.getMobile() + " | 验证码: " + accessSmsDTO.getCaptcha() + " | 错误原因: " + e.getMessage());
            appSmsLog.setResData("错误, 未知异常, Message: " + e.getMessage());
        }
        // 每日短信记录
        accountOTS.saveUserSmsDaily(accessSmsDTO);
        return ServerResult.build(OperateStatus.SUCCESS);
    }

}
