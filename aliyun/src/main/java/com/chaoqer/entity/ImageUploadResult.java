package com.chaoqer.entity;

import lombok.Data;

@Data
public class ImageUploadResult {

    // 图片ID
    private String imageId;
    // 上传地址
    private String uploadAddress;
    // 图片地址
    private String imageURL;

}
