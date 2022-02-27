package com.chaoqer.server;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ListObjectsV2Request;
import com.aliyun.oss.model.ListObjectsV2Result;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.auth.sts.AssumeRoleRequest;
import com.aliyuncs.auth.sts.AssumeRoleResponse;
import com.chaoqer.common.entity.aliyun.FileDirDTO;
import com.chaoqer.common.util.CommonUtil;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import com.chaoqer.entity.StsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import vip.toby.rpc.annotation.RpcServer;
import vip.toby.rpc.annotation.RpcServerMethod;
import vip.toby.rpc.entity.RpcType;
import vip.toby.rpc.entity.ServerResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RpcServer(value = "aliyun-dir", type = RpcType.SYNC, threadNum = 4)
public class AliyunDirServer {

    private final static Logger logger = LoggerFactory.getLogger(AliyunDirServer.class);

    @Value("${aliyun.oss.endpoint}")
    private String ossEndpoint;
    @Value("${aliyun.oss.bucket-name}")
    private String ossBucketName;
    @Value("${aliyun.oss.host}")
    private String ossHost;
    @Value("${aliyun.oss.role-list-arn}")
    private String ossRoleListArn;

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private IAcsClient stsClient;
    @Autowired
    private Environment env;

    @RpcServerMethod(allowDuplicate = true)
    public ServerResult getFileListByDir(FileDirDTO fileDirDTO) {
        List<String> fileList = new ArrayList<>();
        String dir = fileDirDTO.getFileDir();
        try {
            StsResult stsResult = getOssListRoleStsResult();
            if (stsResult != null) {
                OSS oss = new OSSClientBuilder().build(ossEndpoint, stsResult.getAccessKeyId(), stsResult.getAccessKeySecret(), stsResult.getSecurityToken());
                ListObjectsV2Result listObjectsV2Result = oss.listObjectsV2(new ListObjectsV2Request(ossBucketName).withPrefix("audio/room/".concat(dir).concat("/")).withMaxKeys(500));
                for (OSSObjectSummary ossObjectSummary : listObjectsV2Result.getObjectSummaries()) {
                    if (!ossObjectSummary.getKey().equals("audio/room/".concat(dir).concat("/"))) {
                        fileList.add(ossHost.concat("/").concat(ossObjectSummary.getKey()));
                    }
                }
                return ServerResult.buildSuccessResult(fileList);
            }
        } catch (Exception e) {
            logger.error("获取文件夹文件列表地址失败, {}", e.getMessage(), e);
        }
        return ServerResult.buildFailureMessage("获取图片上传地址失败");
    }

    /**
     * 获取sts凭证
     */
    private StsResult getOssListRoleStsResult() {
        StsResult stsResult = RedisUtil.getObject(redisTemplate, RedisKeyGenerator.getOssListRoleStsKey(), StsResult.class);
        if (stsResult == null) {
            try {
                AssumeRoleRequest request = new AssumeRoleRequest();
                request.setSysRegionId("cn-shenzhen");
                request.setRoleArn(ossRoleListArn);
                request.setRoleSessionName(CommonUtil.getEnvironmentName(env));
                // 30分钟有效期
                request.setDurationSeconds(30 * 60L);
                AssumeRoleResponse response = stsClient.getAcsResponse(request);
                stsResult = new StsResult();
                stsResult.setAccessKeyId(response.getCredentials().getAccessKeyId());
                stsResult.setAccessKeySecret(response.getCredentials().getAccessKeySecret());
                stsResult.setSecurityToken(response.getCredentials().getSecurityToken());
                // 本地缓存29分钟
                RedisUtil.setObject(redisTemplate, RedisKeyGenerator.getOssListRoleStsKey(), stsResult, 29, TimeUnit.MINUTES);
                return stsResult;
            } catch (Exception e) {
                logger.error("获取list list sts凭证失败, {}", e.getMessage(), e);
            }
        }
        return stsResult;
    }

}
