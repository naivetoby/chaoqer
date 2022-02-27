package com.chaoqer.controller;

import com.chaoqer.client.aliyun.AliyunDirClient;
import com.chaoqer.client.aliyun.AliyunImageClient;
import com.chaoqer.common.entity.aliyun.FileDirDTO;
import com.chaoqer.common.entity.aliyun.ImageUploadDTO;
import com.chaoqer.common.entity.base.Authed;
import com.chaoqer.common.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping(value = "aliyun", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
public class AliyunController {

    @Autowired
    private AliyunImageClient aliyunImageClient;
    @Autowired
    private AliyunDirClient aliyunDirClient;

    /**
     * 创建上传图片凭证
     */
    @RequestMapping(method = RequestMethod.POST, path = "image/upload")
    public String createImageUpload(
            HttpServletRequest request,
            HttpServletResponse response,
            @Validated @RequestBody ImageUploadDTO imageUploadDTO,
            @RequestAttribute Authed authed
    ) {
        return ResponseUtil.createRpcResult(request, response, aliyunImageClient.createUpload(authed.buildDTO(imageUploadDTO)));
    }

    /**
     * 获取文件夹列表地址
     */
    @RequestMapping(method = RequestMethod.GET, path = "dir/list", consumes = MediaType.ALL_VALUE)
    public String createImageUpload(
            HttpServletRequest request,
            HttpServletResponse response,
            @Validated FileDirDTO fileDirDTO,
            @RequestAttribute Authed authed
    ) {
        return ResponseUtil.createRpcResult(request, response, aliyunDirClient.getFileListByDir(authed.buildDTO(fileDirDTO)));
    }

}
