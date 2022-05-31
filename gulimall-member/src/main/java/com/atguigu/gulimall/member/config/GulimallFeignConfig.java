package com.atguigu.gulimall.member.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Peter
 * @date 2022/5/24 20:44
 * @description feign在远程调用会丢失请求头(Cookie)问题==>自定义RequestInterceptor解决。
 */
@Configuration
public class GulimallFeignConfig {

    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor() {

        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                //System.out.println("feign远程之前先进行RequestInterceptor.apply...");
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    System.out.println("RequestInterceptor线程..." + Thread.currentThread().getName());
                    //老请求
                    HttpServletRequest request = attributes.getRequest();
                    if (request != null) {
                        String cookie = request.getHeader("Cookie");
                        //同步老请求的cookie至新请求
                        template.header("Cookie", cookie);
                    }
                }
            }
        };
    }


}
