package com.chaoqer.common.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jsoup.Jsoup;
import org.mozilla.universalchardet.UniversalDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用HTTP工具类
 *
 * @author toby
 */
public class HttpClientUtil {

    private final static Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);

    public static String get(String url, List<BasicNameValuePair> headers) {
        return get(url, headers, StandardCharsets.UTF_8.name(), 3, 0);
    }

    public static String get(String url, List<BasicNameValuePair> headers, String charset) {
        return get(url, headers, charset, 3, 0);
    }

    public static String get(String url, List<BasicNameValuePair> headers, int retryTotal) {
        return get(url, headers, StandardCharsets.UTF_8.name(), retryTotal, 0);
    }

    private static String get(String url, List<BasicNameValuePair> headers, String charset, int retryTotal, int count) {
        // 失败重试
        if (count > retryTotal) {
            return null;
        }

        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        HttpGet request = null;
        HttpEntity entity = null;
        try {
            // 格式化指定编码
            charset = getDefaultCharset(charset);
            // 创建http连接
            client = defaultHttpClient();
            // 设置请求延时
            RequestConfig config = RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES).setConnectionRequestTimeout(30000).setConnectTimeout(30000).setSocketTimeout(30000).build();
            // 新建POST请求
            request = new HttpGet(url);
            request.setConfig(config);
            // 设置Header
            if (headers != null && !headers.isEmpty()) {
                for (BasicNameValuePair basicNameValuePair : headers) {
                    request.setHeader(basicNameValuePair.getName(), basicNameValuePair.getValue());
                }
            }

            logger.debug(request.toString());

            // 开始请求
            response = client.execute(request);
            // 根据状态码进行判断
            int statusCode = response.getStatusLine().getStatusCode();
            // 正常
            if (statusCode == HttpStatus.SC_OK) {
                entity = response.getEntity();
                // 返回的文档类型
                if (entity != null) {
                    byte[] data = IOUtils.toByteArray(entity.getContent());
                    return new String(data, getCharset(data, entity.getContentType()));
                }
            } else {
                logger.error("get exception: " + url + ", statusCode: " + statusCode);
            }
        } catch (Exception e) {
            logger.error("get exception: " + url + ", message: " + e.getMessage());
            get(url, headers, charset, retryTotal, ++count);
        } finally {
            closeClient(client, request, response, entity);
        }
        return null;
    }

    public static String post(String url, List<BasicNameValuePair> headers, List<BasicNameValuePair> inputs) {
        return post(url, headers, inputs, StandardCharsets.UTF_8.name(), 3, 0);
    }

    public static String post(String url, List<BasicNameValuePair> headers, List<BasicNameValuePair> inputs, String charset) {
        return post(url, headers, inputs, charset, 3, 0);
    }

    public static String post(String url, List<BasicNameValuePair> headers, List<BasicNameValuePair> inputs, int retryTotal) {
        return post(url, headers, inputs, StandardCharsets.UTF_8.name(), retryTotal, 0);
    }

    private static String post(String url, List<BasicNameValuePair> headers, List<BasicNameValuePair> inputs, String charset, int retryTotal, int count) {
        // 失败重试
        if (count > retryTotal) {
            return null;
        }

        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        HttpPost request = null;
        HttpEntity entity = null;
        try {
            // 格式化指定编码
            charset = getDefaultCharset(charset);
            // 创建http连接
            client = defaultHttpClient();
            // 设置请求延时
            RequestConfig config = RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES).setConnectionRequestTimeout(30000).setConnectTimeout(30000).setSocketTimeout(30000).build();
            // 新建POST请求
            request = new HttpPost(url);
            request.setConfig(config);
            // 设置Header
            if (headers != null && !headers.isEmpty()) {
                for (BasicNameValuePair basicNameValuePair : headers) {
                    request.setHeader(basicNameValuePair.getName(), basicNameValuePair.getValue());
                }
            }
            // 设置参数
            if (inputs != null && !inputs.isEmpty()) {
                request.setEntity(new UrlEncodedFormEntity(inputs, StandardCharsets.UTF_8.name()));
            }

            logger.debug(request.toString());

            // 开始请求
            response = client.execute(request);
            // 根据状态码进行判断
            int statusCode = response.getStatusLine().getStatusCode();
            // 正常
            if (statusCode == HttpStatus.SC_OK) {
                entity = response.getEntity();
                // 返回的文档类型
                if (entity != null) {
                    byte[] data = IOUtils.toByteArray(entity.getContent());
                    return new String(data, getCharset(data, entity.getContentType()));
                }
            } else {
                logger.error("post exception: " + url + ", statusCode: " + statusCode);
            }
        } catch (Exception e) {
            logger.error("post exception: " + url + ", message: " + e.getMessage());
            post(url, headers, inputs, charset, retryTotal, ++count);
        } finally {
            closeClient(client, request, response, entity);
        }
        return null;
    }

    public static String post(String url, List<BasicNameValuePair> headers, String payload) {
        return post(url, headers, payload, StandardCharsets.UTF_8.name(), 3, 0);
    }

    public static String post(String url, List<BasicNameValuePair> headers, String payload, String charset) {
        return post(url, headers, payload, charset, 3, 0);
    }

    public static String post(String url, List<BasicNameValuePair> headers, String payload, int retryTotal) {
        return post(url, headers, payload, StandardCharsets.UTF_8.name(), retryTotal, 0);
    }

    private static String post(String url, List<BasicNameValuePair> headers, String payload, String charset, int retryTotal, int count) {
        // 失败重试
        if (count > retryTotal) {
            return null;
        }

        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        HttpPost request = null;
        HttpEntity entity = null;
        try {
            // 格式化指定编码
            charset = getDefaultCharset(charset);
            // 创建http连接
            client = defaultHttpClient();
            // 设置请求延时
            RequestConfig config = RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES).setConnectionRequestTimeout(30000).setConnectTimeout(30000).setSocketTimeout(30000).build();
            // 新建POST请求
            request = new HttpPost(url);
            request.setConfig(config);
            // 设置Header
            if (headers != null && !headers.isEmpty()) {
                for (BasicNameValuePair basicNameValuePair : headers) {
                    request.setHeader(basicNameValuePair.getName(), basicNameValuePair.getValue());
                }
            }
            // 设置参数
            if (StringUtils.isNoneBlank(payload)) {
                request.setEntity(new StringEntity(payload, charset));
            }

            logger.debug(request.toString());

            // 开始请求
            response = client.execute(request);
            // 根据状态码进行判断
            int statusCode = response.getStatusLine().getStatusCode();
            // 正常
            if (statusCode == HttpStatus.SC_OK) {
                entity = response.getEntity();
                // 返回的文档类型
                if (entity != null) {
                    byte[] data = IOUtils.toByteArray(entity.getContent());
                    return new String(data, getCharset(data, entity.getContentType()));
                }
            } else {
                logger.error("post exception: " + url + ", statusCode: " + statusCode);
            }
        } catch (Exception e) {
            logger.error("post exception: " + url + ", message: " + e.getMessage());
            post(url, headers, payload, charset, retryTotal, ++count);
        } finally {
            closeClient(client, request, response, entity);
        }
        return null;
    }

    public static String put(String url, List<BasicNameValuePair> headers, JSONObject inputs) {
        return put(url, headers, null, StandardCharsets.UTF_8.name(), 3, 0);
    }

    public static String put(String url, List<BasicNameValuePair> headers, JSONObject inputs, String charset) {
        return put(url, headers, inputs, charset, 3, 0);
    }

    public static String put(String url, List<BasicNameValuePair> headers, JSONObject inputs, int retryTotal) {
        return put(url, headers, inputs, StandardCharsets.UTF_8.name(), retryTotal, 0);
    }

    private static String put(String url, List<BasicNameValuePair> headers, JSONObject inputs, String charset, int retryTotal, int count) {
        // 失败重试
        if (count > retryTotal) {
            return null;
        }

        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        HttpPut request = null;
        HttpEntity entity = null;
        try {
            // 格式化指定编码
            charset = getDefaultCharset(charset);
            // 创建http连接
            client = defaultHttpClient();
            // 设置请求延时
            RequestConfig config = RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES).setConnectionRequestTimeout(30000).setConnectTimeout(30000).setSocketTimeout(30000).build();
            // 新建PUT请求
            request = new HttpPut(url);
            request.setConfig(config);
            // 设置Header
            if (headers != null && !headers.isEmpty()) {
                for (BasicNameValuePair basicNameValuePair : headers) {
                    request.setHeader(basicNameValuePair.getName(), basicNameValuePair.getValue());
                }
            }
            // 设置参数
            if (inputs != null) {
                request.setEntity(new StringEntity(inputs.toJSONString(), StandardCharsets.UTF_8.name()));
            }

            logger.debug(request.toString());

            // 开始请求
            response = client.execute(request);
            // 根据状态码进行判断
            int statusCode = response.getStatusLine().getStatusCode();
            // 正常
            if (statusCode == HttpStatus.SC_OK) {
                entity = response.getEntity();
                // 返回的文档类型
                if (entity != null) {
                    byte[] data = IOUtils.toByteArray(entity.getContent());
                    return new String(data, getCharset(data, entity.getContentType()));
                }
            } else {
                logger.error("put exception: " + url + ", statusCode: " + statusCode);
            }
        } catch (Exception e) {
            logger.error("put exception: " + url + ", message: " + e.getMessage());
            put(url, headers, inputs, charset, retryTotal, ++count);
        } finally {
            closeClient(client, request, response, entity);
        }
        return null;
    }

    public static String getXML(String url) {
        return getXML(url, StandardCharsets.UTF_8.name(), 3, 0);
    }

    public static String getXML(String url, String charset) {
        return getXML(url, charset, 3, 0);
    }

    public static String getXML(String url, int retryTotal) {
        return getXML(url, StandardCharsets.UTF_8.name(), retryTotal, 0);
    }

    private static String getXML(String url, String charset, int retryTotal, int count) {
        // 失败重试
        if (count > retryTotal) {
            return "";
        }

        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        HttpGet request = null;
        HttpEntity entity = null;
        try {
            // 格式化指定编码
            charset = getDefaultCharset(charset);
            // 创建http连接
            client = defaultHttpClient();
            // 设置请求延时
            RequestConfig config = RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES).setConnectionRequestTimeout(30000).setConnectTimeout(30000).setSocketTimeout(30000).build();
            // 新建GET请求
            request = new HttpGet(url);
            request.setConfig(config);

            logger.debug(request.toString());

            // 开始请求
            response = client.execute(request);
            // 根据状态码进行判断
            int statusCode = response.getStatusLine().getStatusCode();
            // 正常
            if (statusCode == HttpStatus.SC_OK) {
                entity = response.getEntity();
                if (entity != null) {
                    // 通过编码返回字符串
                    byte[] data = IOUtils.toByteArray(entity.getContent());
                    return unicodeStr2String(new String(data, getCharset(data, entity.getContentType())));
                }
            } else {
                logger.error("getXML exception: " + url + ", statusCode: " + statusCode);
            }
        } catch (Exception e) {
            logger.error("getXML exception: " + url + ", message: " + e.getMessage());
            getXML(url, charset, retryTotal, ++count);
        } finally {
            closeClient(client, request, response, entity);
        }
        return "";
    }

    public static String postXML(String url, String xml) {
        return postXML(url, xml, StandardCharsets.UTF_8.name(), 3, 0);
    }

    public static String postXML(String url, String xml, String charset) {
        return postXML(url, xml, charset, 3, 0);
    }

    public static String postXML(String url, String xml, int retryTotal) {
        return postXML(url, xml, StandardCharsets.UTF_8.name(), retryTotal, 0);
    }

    private static String postXML(String url, String xml, String charset, int retryTotal, int count) {
        // 失败重试
        if (count > retryTotal) {
            return "";
        }

        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        HttpPost request = null;
        HttpEntity entity = null;
        try {
            // 格式化指定编码
            charset = getDefaultCharset(charset);
            // 创建http连接
            client = defaultHttpClient();
            // 设置请求延时
            RequestConfig config = RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES).setConnectionRequestTimeout(30000).setConnectTimeout(30000).setSocketTimeout(30000).build();
            // 新建POST请求
            request = new HttpPost(url);
            request.setConfig(config);
            // 设置参数
            StringEntity stringEntity = new StringEntity(xml, charset);
            request.setEntity(stringEntity);

            logger.debug(request.toString());

            // 开始请求
            response = client.execute(request);
            // 根据状态码进行判断
            int statusCode = response.getStatusLine().getStatusCode();
            // 正常
            if (statusCode == HttpStatus.SC_OK) {
                entity = response.getEntity();
                if (entity != null) {
                    // 通过编码返回字符串
                    byte[] data = IOUtils.toByteArray(entity.getContent());
                    return unicodeStr2String(new String(data, getCharset(data, entity.getContentType())));
                }
            } else {
                logger.error("postXML exception: " + url + ", statusCode: " + statusCode);
            }
        } catch (Exception e) {
            logger.error("postXML exception: " + url + ", message: " + e.getMessage());
            postXML(url, xml, charset, retryTotal, ++count);
        } finally {
            closeClient(client, request, response, entity);
        }
        return "";
    }


    private static String getBaseUrl(String url) {
        return "http://".concat(url.replace("http://", "").split("/")[0]).concat("/");
    }

    private static String getRedirectUrl(String baseUrl, String url) {
        if (StringUtils.isNotBlank(url)) {
            if (url.startsWith("/")) {
                return getBaseUrl(baseUrl).concat(url.substring(1));
            }
            if (url.startsWith("http://") || url.startsWith("https://")) {
                return url;
            }
            return baseUrl.substring(0, baseUrl.lastIndexOf("/")).concat("/").concat(url);
        }
        return baseUrl;
    }

    private static String getCharset(byte[] data, Header contentType) {
        String charset = null;
        try {
            // 先通过contentType尝试获取编码
            if (contentType != null) {
                String value = contentType.getValue();
                if (StringUtils.isNotBlank(value)) {
                    charset = matchCharset(value + ";");
                }
            }
            // 通过contentType获取编码失败,通过网页获取
            if (StringUtils.isBlank(charset)) {
                charset = getWabPageCharset(data);
                // 通过网页获取编码失败,通过字节码获取
                if (StringUtils.isBlank(charset)) {
                    InputStream is = new ByteArrayInputStream(data);
                    UniversalDetector detector = new UniversalDetector(null);
                    byte[] buf = new byte[1024];
                    int read;
                    while ((read = is.read(buf)) > 0 && !detector.isDone()) {
                        detector.handleData(buf, 0, read);
                    }
                    detector.dataEnd();
                    charset = detector.getDetectedCharset();
                    detector.reset();
                    is.close();
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return getDefaultCharset(charset);
    }

    // 获得文本字符串的编码格式
    private static String getWabPageCharset(byte[] data) throws IOException {
        // 通过网页获取编码
        org.jsoup.nodes.Document doc = Jsoup.parse(new String(data, StandardCharsets.UTF_8));
        for (org.jsoup.nodes.Element element : doc.select("meta[http-equiv=Content-Type]")) {
            return matchCharset(element.toString());
        }
        return null;
    }

    // 获得页面字符
    private static String matchCharset(String content) {
        Pattern p = Pattern.compile("[cC][hH][aA][rR][sS][eE][tT] *= *\"?(.+?)[ ;\"]+?");
        Matcher m = p.matcher(content);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private static String getDefaultCharset(String charset) {
        if (StringUtils.isNotBlank(charset)) {
            try {
                charset = Charset.forName(charset.toLowerCase()).name();
            } catch (Exception e) {
                charset = StandardCharsets.UTF_8.name();
            }
        } else {
            charset = StandardCharsets.UTF_8.name();
        }
        return charset;
    }

    private static void closeClient(CloseableHttpClient client, AbstractExecutionAwareRequest request, CloseableHttpResponse response, HttpEntity entity) {
        try {
            if (request != null) {
                request.abort();
            }
            if (response != null) {
                response.close();
            }
            if (entity != null) {
                EntityUtils.consume(entity);
            }
            if (client != null) {
                client.close();
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    //解析报文，根据末节点名称获取值
    public static JSONObject parseXMLToJSON(String xmlResult) {
        JSONObject result = new JSONObject();
        if (StringUtils.isBlank(xmlResult)) {
            return null;
        }
        try {
            Document doc = DocumentHelper.parseText(xmlResult);
            getCode(doc.getRootElement(), result);
            if (result.containsKey("response")) {
                return (JSONObject) JSON.parse(result.getString("response"));
            }
        } catch (DocumentException e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }

    private static CloseableHttpClient defaultHttpClient() {
        return HttpClients.custom()
                .setUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1")
                .disableRedirectHandling()
                .build();
    }

    /**
     * unicode 转字符串
     *
     * @param unicode 全为 Unicode 的字符串
     * @return
     */
    private static String unicode2String(String unicode) {
        StringBuffer string = new StringBuffer();
        String[] hex = unicode.split("\\\\u");

        for (int i = 1; i < hex.length; i++) {
            // 转换出每一个代码点
            int data = Integer.parseInt(hex[i], 16);
            // 追加成string
            string.append((char) data);
        }

        return string.toString();
    }

    /**
     * 含有unicode 的字符串转一般字符串
     *
     * @param unicodeStr 混有 Unicode 的字符串
     * @return
     */
    private static String unicodeStr2String(String unicodeStr) {
        int length = unicodeStr.length();
        int count = 0;
        //正则匹配条件，可匹配“\\u”1到4位，一般是4位可直接使用 String regex = "\\\\u[a-f0-9A-F]{4}";
        String regex = "\\\\u[a-f0-9A-F]{1,4}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(unicodeStr);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String oldChar = matcher.group();//原本的Unicode字符
            String newChar = unicode2String(oldChar);//转换为普通字符
            // int index = unicodeStr.indexOf(oldChar);
            // 在遇见重复出现的unicode代码的时候会造成从源字符串获取非unicode编码字符的时候截取索引越界等
            int index = matcher.start();

            sb.append(unicodeStr.substring(count, index));//添加前面不是unicode的字符
            sb.append(newChar);//添加转换后的字符
            count = index + oldChar.length();//统计下标移动的位置
        }
        sb.append(unicodeStr.substring(count, length));//添加末尾不是Unicode的字符
        return sb.toString();
    }

    private static void getCode(Element root, JSONObject result) {
        if (root.elements() != null) {
            List<Element> list = root.elements();//如果当前跟节点有子节点，找到子节点
            for (Element e : list) {//遍历每个节点
                if (e.elements().size() > 0) {
                    getCode(e, result);//当前节点不为空的话，递归遍历子节点；
                }
                // 如果为叶子节点，那么直接把名字和值放入map
                if (e.elements().size() == 0) {
                    result.put(e.getName(), e.getTextTrim());
                }
            }
        }
    }

}
