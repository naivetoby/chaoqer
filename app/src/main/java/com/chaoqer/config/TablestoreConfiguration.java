package com.chaoqer.config;

import com.alicloud.openservices.tablestore.AsyncClient;
import com.alicloud.openservices.tablestore.ClientConfiguration;
import com.alicloud.openservices.tablestore.SyncClient;
import com.chaoqer.common.util.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
class TablestoreConfiguration {

    private final static Logger logger = LoggerFactory.getLogger(TablestoreConfiguration.class);

    @Value("${aliyun.ots.access-key.id}")
    private String accessKeyId;
    @Value("${aliyun.ots.access-key.secret}")
    private String accessKeySecret;
    @Value("${aliyun.ots.data.endpoint}")
    private String dataEndpoint;
    @Value("${aliyun.ots.data.endpoint-public}")
    private String dataEndpointPublic;
    @Value("${aliyun.ots.data.instance-name}")
    private String dataInstanceName;
    @Value("${aliyun.ots.log.endpoint}")
    private String logEndpoint;
    @Value("${aliyun.ots.log.endpoint-public}")
    private String logEndpointPublic;
    @Value("${aliyun.ots.log.instance-name}")
    private String logInstanceName;
    @Autowired
    private Environment env;

    @Bean
    public static ClientConfiguration clientConfiguration() {
        // ClientConfiguration提供了很多配置项，以下只列举部分。
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        // 设置建立连接的超时时间
        clientConfiguration.setConnectionTimeoutInMillisecond(5000);
        // 设置socket超时时间
        clientConfiguration.setSocketTimeoutInMillisecond(5000);
        return clientConfiguration;
    }

    @Bean
    public SyncClient dataSyncClient(ClientConfiguration clientConfiguration) {
        return createSyncClient(dataEndpoint, dataEndpointPublic, dataInstanceName, clientConfiguration);
    }

    @Bean
    public AsyncClient dataAsyncClient(ClientConfiguration clientConfiguration) {
        return createAsyncClient(dataEndpoint, dataEndpointPublic, dataInstanceName, clientConfiguration);
    }

    @Bean
    public SyncClient logSyncClient(ClientConfiguration clientConfiguration) {
        return createSyncClient(logEndpoint, logEndpointPublic, logInstanceName, clientConfiguration);
    }

    @Bean
    public AsyncClient logAsyncClient(ClientConfiguration clientConfiguration) {
        return createAsyncClient(logEndpoint, logEndpointPublic, logInstanceName, clientConfiguration);
    }

    private SyncClient createSyncClient(String endpoint, String endpointPublic, String instanceName, ClientConfiguration clientConfiguration) {
        if (CommonUtil.isLocalEnvironment(env)) {
            return new SyncClient(endpointPublic, accessKeyId, accessKeySecret, instanceName, clientConfiguration);
        }
        return new SyncClient(endpoint, accessKeyId, accessKeySecret, instanceName, clientConfiguration);
    }

    private AsyncClient createAsyncClient(String endpoint, String endpointPublic, String instanceName, ClientConfiguration clientConfiguration) {
        if (CommonUtil.isLocalEnvironment(env)) {
            return new AsyncClient(endpointPublic, accessKeyId, accessKeySecret, instanceName, clientConfiguration);
        }
        return new AsyncClient(endpoint, accessKeyId, accessKeySecret, instanceName, clientConfiguration);
    }

}
