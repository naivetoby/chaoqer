package com.chaoqer.config;

import com.chaoqer.interceptor.ClientLoginInterceptor;
import com.chaoqer.interceptor.H5LoginInterceptor;
import com.chaoqer.interceptor.ParamVerifyInterceptor;
import com.chaoqer.interceptor.UserLoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebAppConfig implements WebMvcConfigurer {

    @Autowired
    private ClientLoginInterceptor clientLoginInterceptor;
    @Autowired
    private UserLoginInterceptor userLoginInterceptor;
    @Autowired
    private ParamVerifyInterceptor paramVerifyInterceptor;
    @Autowired
    private H5LoginInterceptor h5LoginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(clientLoginInterceptor).addPathPatterns("/**");
        registry.addInterceptor(userLoginInterceptor).addPathPatterns("/**");
        registry.addInterceptor(paramVerifyInterceptor).addPathPatterns("/**");
//        registry.addInterceptor(h5LoginInterceptor).addPathPatterns("/h5/**");
    }

}
