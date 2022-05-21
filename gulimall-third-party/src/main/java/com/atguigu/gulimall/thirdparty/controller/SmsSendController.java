package com.atguigu.gulimall.thirdparty.controller;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.thirdparty.component.SmsComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/5/18 15:43
 * @description Usage
 */
@RestController
@RequestMapping("/sms")
public class SmsSendController {

    @Autowired
    SmsComponent smsComponent;

    /**
     * 提供给别人的服务使用，不直接给用户使用
     * @param phone
     * @param code
     * @return
     */
    @GetMapping("/sendCode")
    public R sendCode(@RequestParam("phone") String phone, @RequestParam("code") String code) {

        smsComponent.sendSmsCode(phone, code);

        return R.ok();
    }
}
