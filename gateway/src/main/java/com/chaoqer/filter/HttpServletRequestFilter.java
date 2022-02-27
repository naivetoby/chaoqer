package com.chaoqer.filter;

import com.chaoqer.util.HttpContextUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/***
 * HttpServletRequest 过滤器
 * 解决: request.getInputStream()只能读取一次的问题
 * 目标: 流可重复读
 */
@Component
@Order(10000)
public class HttpServletRequestFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        ServletRequest requestWrapper = null;
        if (servletRequest instanceof HttpServletRequest && APPLICATION_JSON_VALUE.equals(((HttpServletRequest) servletRequest).getHeader("Content-Type"))) {
            requestWrapper = new RequestWrapper((HttpServletRequest) servletRequest);
        }
        if (null == requestWrapper) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            filterChain.doFilter(requestWrapper, servletResponse);
        }
    }

    @Override
    public void destroy() {

    }

    /***
     * HttpServletRequest 包装器
     * 解决: request.getInputStream()只能读取一次的问题
     * 目标: 流可重复读
     */
    static class RequestWrapper extends HttpServletRequestWrapper {

        /**
         * 请求体
         */
        private String mBody;

        public RequestWrapper(HttpServletRequest request) {
            super(request);
            // 将body数据存储起来
            mBody = getBody(request);
        }

        /**
         * 获取请求体
         *
         * @param request 请求
         * @return 请求体
         */
        private String getBody(HttpServletRequest request) {
            return HttpContextUtils.getBodyString(request);
        }

        /**
         * 获取请求体
         *
         * @return 请求体
         */
        public String getBody() {
            return mBody;
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        }

        @Override
        public ServletInputStream getInputStream() {
            // 创建字节数组输入流
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(mBody.getBytes(StandardCharsets.UTF_8));

            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return false;
                }

                @Override
                public boolean isReady() {
                    return false;
                }

                @Override
                public void setReadListener(ReadListener readListener) {

                }

                @Override
                public int read() {
                    return byteArrayInputStream.read();
                }
            };
        }
    }

}
