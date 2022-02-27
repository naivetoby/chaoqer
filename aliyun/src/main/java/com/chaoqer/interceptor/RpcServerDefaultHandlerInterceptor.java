package com.chaoqer.interceptor;

import com.chaoqer.common.util.DigestUtil;
import com.chaoqer.common.util.RedisKeyGenerator;
import com.chaoqer.common.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import vip.toby.rpc.server.RpcServerBaseHandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Component
public class RpcServerDefaultHandlerInterceptor extends RpcServerBaseHandlerInterceptor {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public boolean rpcDuplicateHandle(String method, String correlationId) {
        // 请求ID相同
        return !RedisUtil.setLockKeyIfAbsent(redisTemplate, RedisKeyGenerator.getSimpleRpcCorrelationIdKey(method.concat(correlationId)), 30);
    }

    @Override
    public boolean duplicateHandle(String method, Object data) {
        // 参数相同的请求是否重复
        return !RedisUtil.setLockKeyIfAbsent(redisTemplate, RedisKeyGenerator.getSimpleRpcDetailMd5Key(method.concat(DigestUtil.md5(data.toString()))), 500, TimeUnit.MILLISECONDS);
    }

}
