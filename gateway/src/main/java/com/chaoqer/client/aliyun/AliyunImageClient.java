package com.chaoqer.client.aliyun;

import com.chaoqer.common.entity.aliyun.ImageUploadDTO;
import vip.toby.rpc.annotation.RpcClient;
import vip.toby.rpc.annotation.RpcClientMethod;
import vip.toby.rpc.entity.RpcResult;

@RpcClient(value = "aliyun-image")
public interface AliyunImageClient {

    @RpcClientMethod
    RpcResult createUpload(ImageUploadDTO imageUploadDTO);

}
