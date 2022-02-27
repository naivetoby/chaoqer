package com.chaoqer.client.account;

import com.chaoqer.common.entity.account.AccountAuthDTO;
import com.chaoqer.common.entity.account.CaptchaDTO;
import com.chaoqer.common.entity.account.CaptchaLoginDTO;
import com.chaoqer.common.entity.base.AuthedDTO;
import vip.toby.rpc.annotation.RpcClient;
import vip.toby.rpc.annotation.RpcClientMethod;
import vip.toby.rpc.entity.RpcResult;

@RpcClient(value = "account")
public interface AccountClient {

    @RpcClientMethod
    RpcResult accountAuth(AccountAuthDTO accountAuthDTO);

    @RpcClientMethod
    RpcResult getAccountSecret(AuthedDTO authedDTO);

    @RpcClientMethod
    RpcResult getCaptcha(CaptchaDTO captchaDTO);

    @RpcClientMethod
    RpcResult captchaLogin(CaptchaLoginDTO captchaLoginDTO);

    @RpcClientMethod
    RpcResult logout(AuthedDTO authedDTO);

}
