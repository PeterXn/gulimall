package com.atguigu.gulimall.seckill.config;

import com.atguigu.gulimall.seckill.interceptor.LoginUserInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Peter
 * @date 2022/5/24 9:41
 * @description Usage
 */
@Configuration
public class SeckillWebConfig implements WebMvcConfigurer {

    @Autowired
    LoginUserInterceptor loginUserInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //拦截当前项目下的所有请求，判断是否登录
        registry.addInterceptor(loginUserInterceptor).addPathPatterns("/**");
    }
}
