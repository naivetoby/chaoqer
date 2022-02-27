package com.chaoqer.controller;

import com.chaoqer.annotation.UserLoginSkip;
import com.chaoqer.client.account.AccountClient;
import com.chaoqer.common.entity.account.CaptchaDTO;
import com.chaoqer.common.entity.account.CaptchaLoginDTO;
import com.chaoqer.common.entity.base.Authed;
import com.chaoqer.common.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping(value = "account", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
public class AccountController {

    @Autowired
    private AccountClient accountClient;

    /**
     * 获取短信验证码
     */
    @UserLoginSkip
    @RequestMapping(method = RequestMethod.POST, path = "captcha")
    public String getCaptcha(
            HttpServletRequest request,
            HttpServletResponse response,
            @Validated @RequestBody CaptchaDTO captchaDTO,
            @RequestAttribute Authed authed
    ) {
        return ResponseUtil.createRpcResult(request, response, accountClient.getCaptcha(authed.buildDTO(captchaDTO, true)));
    }

    /**
     * 短信验证码登录/注册账号
     */
    @UserLoginSkip
    @RequestMapping(method = RequestMethod.POST, path = "login/captcha")
    public String captchaLogin(
            HttpServletRequest request,
            HttpServletResponse response,
            @Validated @RequestBody CaptchaLoginDTO captchaLoginDTO,
            @RequestAttribute Authed authed
    ) {
        return ResponseUtil.createRpcResult(request, response, accountClient.captchaLogin(authed.buildDTO(captchaLoginDTO, true)));
    }

    /**
     * 退出登录
     */
    @RequestMapping(method = RequestMethod.POST, path = "logout")
    public String logout(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestAttribute Authed authed
    ) {
        return ResponseUtil.createRpcResult(request, response, accountClient.logout(authed.buildDTO(true)));
    }

}
