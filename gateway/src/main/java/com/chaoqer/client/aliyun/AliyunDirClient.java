package com.chaoqer.client.aliyun;

import com.chaoqer.common.entity.aliyun.FileDirDTO;
import vip.toby.rpc.annotation.RpcClient;
import vip.toby.rpc.annotation.RpcClientMethod;
import vip.toby.rpc.entity.RpcResult;

@RpcClient(value = "aliyun-dir")
public interface AliyunDirClient {

    @RpcClientMethod
    RpcResult getFileListByDir(FileDirDTO fileDirDTO);

}
