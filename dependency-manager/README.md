## 统一 Maven 依赖管理

#### 当前依赖版本
 
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.3.5.RELEASE</version>
</parent>

<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    <java.version>1.8</java.version>
    <simple-rpc.version>1.4.3.RELEASE</simple-rpc.version>
    <fastjson.version>1.2.75</fastjson.version>
    <aliyun-log-logback-appender.version>0.1.17</aliyun-log-logback-appender.version>
    <aliyun-java-sdk-core.version>4.5.18</aliyun-java-sdk-core.version>
    <aliyun-java-sdk-vod.version>2.15.12</aliyun-java-sdk-vod.version>
    <aliyun-java-sdk-green.version>3.6.3</aliyun-java-sdk-green.version>
    <aliyun-sdk-oss.version>3.11.2</aliyun-sdk-oss.version>
    <tablestore.version>5.10.3</tablestore.version>
    <lombok.version>1.18.16</lombok.version>
    <mysql-connector-java.version>8.0.22</mysql-connector-java.version>
    <c3p0.version>0.9.5.5</c3p0.version>
    <paoding-rose-jade.version>2.0.u08</paoding-rose-jade.version>
    <commons-lang3.version>3.11</commons-lang3.version>
    <commons-pool2.version>2.9.0</commons-pool2.version>
    <commons-io.version>2.8.0</commons-io.version>
    <commons-math3.version>3.6.1</commons-math3.version>
    <commons-beanutils.version>1.9.4</commons-beanutils.version>
    <cglib.version>3.3.0</cglib.version>
    <httpclient.version>4.5.13</httpclient.version>
    <jsoup.version>1.13.1</jsoup.version>
    <dom4j.version>2.1.3</dom4j.version>
    <javax.servlet-api.version>4.0.1</javax.servlet-api.version>
    <juniversalchardet.version>1.0.3</juniversalchardet.version>
    <libphonenumber.version>8.12.13</libphonenumber.version>
    <guava.version>30.0-jre</guava.version>
    <hibernate-types-52.version>2.10.0</hibernate-types-52.version>
    <gson.version>2.8.6</gson.version>
    <opencc4j.version>1.6.0</opencc4j.version>
    <jpush-client.version>3.4.8</jpush-client.version>
    <jiguang-common.version>1.1.10</jiguang-common.version>
    <vertx-core.version>3.8.0</vertx-core.version>
    <vertx-web.version>3.8.0</vertx-web.version>
    <maven-source-plugin.version>3.2.1</maven-source-plugin.version>
    <maven-compiler-plugin.version>3.8.1</maven-compiler-plugin.version>
    <maven-dependency-plugin.version>3.1.2</maven-dependency-plugin.version>
</properties>
```

#### 如何使用

```xml
<parent>
    <groupId>com.litgee</groupId>
    <artifactId>dependency-manager</artifactId>
    <version>1.3.5.RELEASE</version>
</parent>
```
