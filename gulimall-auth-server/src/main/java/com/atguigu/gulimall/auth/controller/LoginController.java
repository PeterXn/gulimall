package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.feign.ThirdPartyFeignService;
import com.atguigu.gulimall.auth.vo.UserLoginVo;
import com.atguigu.gulimall.auth.vo.UserRegistVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/5/18 9:58
 * @description Usage
 */
@Slf4j
@Controller
public class LoginController {

    @Autowired
    ThirdPartyFeignService thirdPartyFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    MemberFeignService memberFeignService;

    @ResponseBody
    @GetMapping("/sms/sendCode")
    public R sendCode(@RequestParam("phone") String phone) {
        // TODO 1. 接口防刷

        // 60秒不能重发
        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (!StringUtils.isEmpty(redisCode)) {
            long redisTime = Long.parseLong(redisCode.split("_")[1]);
            if ((System.currentTimeMillis() - redisTime) < 60000) {
                // 60000毫秒内不能重发
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            } else {
                //TODO 时间在 60s < time < 10 mim；继续发验证码
            }
        }

        // 2. 生成、拼接redis-key，存入redis key -> sms:code:12345678912
        String code = String.valueOf(System.currentTimeMillis()).substring(7, 13);
        // value -> 123456_System.currentTimeMillis()
        String codeValue = code + "_" + System.currentTimeMillis();
        System.out.println("code = " + code);

        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, codeValue, 10, TimeUnit.MINUTES);

        // 远程调用短信接口
        thirdPartyFeignService.sendCode(phone, code);

        return R.ok();
    }

    /**
     * //TODO 重定向携带数据，利用session原理；将数据放在session中，
     * 只要跳到下一个页面取出数据以后，session里面的数据就会删除。
     * <p>
     * TODO 分布式下session问题。
     *
     * @param vo
     * @param result
     * @param redirectAttributes 模拟重定向携带数据
     * @return
     */
    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result,
                         RedirectAttributes redirectAttributes) {
        Map<String, String> errors = new HashMap<>();
        if (result.hasErrors()) {
            errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));

            //model.addAttribute("errors", errors);
            redirectAttributes.addFlashAttribute("errors", errors);

            // Request method 'POST' not supported
            // 用户注册-->/reg.html[post]-->转发/reg.html ["forward:/reg.html"] (路径映射默认都是get方式访问的)

            // 校验出错，转发到注册页
            return "redirect:http://auth.gulimall.com/reg.html";
        }


        // 1.校验验证码，验证通过才调用远程服务
        String code = vo.getCode();
        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if (StringUtils.isEmpty(redisCode)) {
            // 验证码已过期或不存在
            errors.put("code", "验证码错误或验证码已过期");
            redirectAttributes.addFlashAttribute("errors", errors);

            // 校验出错，转发到注册页
            return "redirect:http://auth.gulimall.com/reg.html";
        } else {
            if (!code.equals(redisCode.split("_")[0])) {
                // 验证码对比失败
                errors.put("code", "验证码错误");
                redirectAttributes.addFlashAttribute("errors", errors);

                // 校验出错，转发到注册页
                return "redirect:http://auth.gulimall.com/reg.html";
            } else {
                // 验证码未过期
                // 删除redis验证码
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());

                // 验证码通过；真正注册。调用远程服务注册。远程服务校验用户名或手机是否占用
                R r = memberFeignService.regist(vo);
                if (r.getCode() != 0) {
                    // 失败
                    errors.put("msg", r.getData("msg",new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("errors", errors);

                    return "redirect:http://auth.gulimall.com/reg.html";
                } else {
                    // 注册成功返回到首页

                    return "redirect:http://auth.gulimall.com/login.html";
                }
            }
        }
    }

    @GetMapping("/login.html")
    public String loginPage(HttpSession session) {
        Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if (attribute == null) {
            //没登录
            return "login";
        } else {
            return "redirect:http://gulimall.com";
        }
    }

    /**
     * 登录
     * @return
     */
    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes, HttpSession session) {

        //远程调用member服务验证
        R r = memberFeignService.login(vo);
        if (r.getCode() == 0) {
            MemberRespVo data = r.getData("data", new TypeReference<MemberRespVo>() {
            });
            //成功
            session.setAttribute(AuthServerConstant.LOGIN_USER, data);
            log.info("登录成功，用户信息为 {}", data.toString());
            return "redirect:http://gulimall.com";
        } else {
            //失败
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", r.getData("msg",new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors", errors);

            return "redirect:http://auth.gulimall.com/login.html";
        }

    }

    @GetMapping(value = "/loguot.html")
    public String logout(HttpServletRequest request) {

        request.getSession().removeAttribute(AuthServerConstant.LOGIN_USER);
        request.getSession().invalidate();

        return "redirect:http://gulimall.com";
    }
}
