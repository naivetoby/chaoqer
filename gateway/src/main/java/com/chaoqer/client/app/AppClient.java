package com.chaoqer.client.app;

import com.chaoqer.common.entity.app.FeedbackDTO;
import com.chaoqer.common.entity.app.ReportDTO;
import com.chaoqer.common.entity.base.AuthedDTO;
import vip.toby.rpc.annotation.RpcClient;
import vip.toby.rpc.annotation.RpcClientMethod;
import vip.toby.rpc.entity.RpcResult;

@RpcClient(value = "app")
public interface AppClient {

    @RpcClientMethod
    RpcResult getLatestVersion(AuthedDTO authedDTO);

    @RpcClientMethod
    RpcResult postFeedback(FeedbackDTO feedbackDTO);

    @RpcClientMethod
    RpcResult postReport(ReportDTO reportDTO);

}
