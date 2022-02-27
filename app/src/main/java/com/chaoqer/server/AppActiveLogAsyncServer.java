package com.chaoqer.server;

import com.chaoqer.common.entity.app.AppUserActiveLogDTO;
import com.chaoqer.repository.AccountOTS;
import com.chaoqer.repository.LogOTS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import vip.toby.rpc.annotation.RpcServer;
import vip.toby.rpc.annotation.RpcServerMethod;
import vip.toby.rpc.entity.OperateStatus;
import vip.toby.rpc.entity.RpcType;
import vip.toby.rpc.entity.ServerResult;

@RpcServer(value = "app-active-log", type = RpcType.ASYNC, threadNum = 4)
public class AppActiveLogAsyncServer {

    @Autowired
    private AccountOTS accountOTS;
    @Autowired
    private LogOTS logOTS;

    // 用户活跃日志
    @RpcServerMethod(allowDuplicate = true)
    public ServerResult createAppUserActiveLog(@Validated AppUserActiveLogDTO appUserActiveLogDTO) {
        if (!appUserActiveLogDTO.isUserLogin()) {
            return ServerResult.build(OperateStatus.SUCCESS);
        }
        // 行为日志
        logOTS.createAppUserActiveLog(appUserActiveLogDTO);
        // 用户权限认证续期(每天)
        accountOTS.asyncUpdateAccountAuthExpireTime(appUserActiveLogDTO.getAuthedUid());
        // 用户活跃更新(每分钟)
        accountOTS.asyncUpdateAccountActiveTime(appUserActiveLogDTO.getAuthedUid(), appUserActiveLogDTO.getClientInfo());
        return ServerResult.build(OperateStatus.SUCCESS);
    }

}
