package com.chaoqer.common.util;

import com.alibaba.fastjson.JSONObject;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.servlet.http.HttpServletRequest;
import java.beans.PropertyVetoException;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 通用工具类
 *
 * @author toby
 */
public class CommonUtil {

    private final static Logger logger = LoggerFactory.getLogger(CommonUtil.class);

    public static String getEnvironmentName(Environment env) {
        String[] activeProfiles = env.getActiveProfiles();
        if (activeProfiles.length > 0) {
            return activeProfiles[0];
        }
        return "prod";
    }

    public static boolean isEnvironment(Environment env, String targetEnvName) {
        if (StringUtils.isNotBlank(targetEnvName)) {
            return getEnvironmentName(env).equals(targetEnvName);
        }
        return false;
    }

    public static boolean isProdEnvironment(Environment env) {
        return isEnvironment(env, "prod");
    }

    public static boolean isDevEnvironment(Environment env) {
        return isEnvironment(env, "dev");
    }

    public static boolean isLocalEnvironment(Environment env) {
        return isEnvironment(env, "local");
    }

    public static String getRelativePath(String name) {
        return System.getProperties().getProperty("user.dir").concat(System.getProperties().getProperty("file.separator")).concat(name);
    }

    public static File getFileByPath(String path, boolean isAbsolute) {
        if (isAbsolute) {
            return new File(path);
        }
        return new File(System.getProperty("user.dir") + System.getProperty("file.separator") + path);
    }

    public static String createLinkString(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        String prestr = "";
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = params.get(key);
            if (i == keys.size() - 1) {// 拼接时，不包括最后一个&字符
                prestr = prestr + key.toLowerCase() + "=" + value;
            } else {
                prestr = prestr + key.toLowerCase() + "=" + value + "&";
            }
        }
        return prestr;
    }

    public static String createLinkString(JSONObject params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        String prestr = "";
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = params.get(key).toString();
            if (i == keys.size() - 1) {// 拼接时，不包括最后一个&字符
                prestr = prestr + key + "=" + "" + value + "";
            } else {
                prestr = prestr + key + "=" + "" + value + "" + "&";
            }
        }
        return prestr;
    }

    public static ComboPooledDataSource createComboPooledDataSource(String host, String port, String db, String user, String password) throws PropertyVetoException {
        ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setDriverClass("com.mysql.cj.jdbc.Driver");
        dataSource.setJdbcUrl("jdbc:mysql://".concat(host).concat(":").concat(port).concat("/").concat(db).concat("?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2b8"));
        dataSource.setUser(user);
        dataSource.setPassword(password);
        dataSource.setAutoCommitOnClose(false);
        dataSource.setCheckoutTimeout(10000);
        dataSource.setMaxIdleTime(60);
        dataSource.setAcquireIncrement(3);
        dataSource.setAcquireRetryAttempts(30);
        dataSource.setAcquireRetryDelay(1000);
        dataSource.setInitialPoolSize(5);
        dataSource.setMinPoolSize(3);
        dataSource.setMaxPoolSize(100);
        dataSource.setIdleConnectionTestPeriod(60);
        return dataSource;
    }

    public static void closeConnection(PreparedStatement pstmt, Connection conn, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
            if (pstmt != null) {
                pstmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static String nullToDefault(String value) {
        return nullToDefault(value, "");
    }

    public static String nullToDefault(String value, String defaultValue) {
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return value;
    }

    public static long nullToDefault(Long value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public static double nullToDefault(Double value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public static int nullToDefault(Integer value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public static boolean nullToDefault(Boolean value) {
        if (value == null) {
            return false;
        }
        return value;
    }

    public static String getHost(HttpServletRequest request) {
        if (request != null) {
            String host = request.getServerName();
            if (StringUtils.isNotBlank(host) && host.contains(".") && !host.equals("127.0.0.1")) {
                return "https://h5" + host.substring(host.indexOf(".")) + "/";
            }
            return "https://local-h5.chaoqer.com/";
        }
        return "https://h5.chaoqer.com/";
    }

    public static String getDomain(HttpServletRequest request) {
        if (request != null) {
            String host = request.getServerName();
            if (StringUtils.isNotBlank(host) && host.contains(".") && !host.equals("127.0.0.1")) {
                return host.substring(host.indexOf("."));
            }
        }
        return ".chaoqer.com";
    }

}