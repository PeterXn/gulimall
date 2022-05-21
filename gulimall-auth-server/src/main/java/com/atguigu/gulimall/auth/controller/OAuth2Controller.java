package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.to.SocialUserTo;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.common.vo.MemberRespVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/5/19 20:45
 * @description 处理社交登录请求
 */
@Controller
@Slf4j
public class OAuth2Controller {

    @Autowired
    MemberFeignService memberFeignService;

    @GetMapping("/oauth2.0/weibo/success")
    public String weibo(@RequestParam("code") String code, HttpSession session) throws Exception {
        //1.根据code换取Access Token
        Map<String, String> headers = new HashMap<>();
        Map<String, String> querys = new HashMap<>();
        Map<String, String> map = new HashMap<>();
        map.put("client_id", "2129105835");
        map.put("client_secret", "201b8aa95794dbb6d52ff914fc8954dc");
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", "http://auth.gulimall.com/oauth2.0/weibo/success");
        map.put("code", code);
        HttpResponse response = HttpUtils.doPost("https://api.weibo.com", "/oauth2/access_token", "post", headers, querys, map);

        //2.处理返回数据
        if (response.getStatusLine().getStatusCode() == 200) {
            //获取到AccessToken
            String json = EntityUtils.toString(response.getEntity());
            SocialUserTo socialUserTo = JSON.parseObject(json, SocialUserTo.class);
            log.info(socialUserTo.toString());

            //知道当前是哪个社交用户。
            //1).如果当前社交用户第一次登录，则为这个社交用户生成一个会员信息账号
            //登录或者注册这个社交用户
            R r = memberFeignService.oauth2Login(socialUserTo);
            if (r.getCode() == 0) {
                //3.登录成功跳转到首页
                //TODO 成功后的处理？
                MemberRespVo data = r.getData("data", new TypeReference<MemberRespVo>() {
                });
                session.setAttribute(AuthServerConstant.LOGIN_USER, data);
                log.info("登录成功，用户信息为 {}", data.toString());

                return "redirect:http://gulimall.com";
            } else {
                //登录失败，重新登录
                return "redirect:http://auth.gulimall.com/login.html";
            }

        } else {
            //登录失败，重新登录
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }
}
