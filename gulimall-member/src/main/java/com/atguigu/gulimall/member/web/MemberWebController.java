package com.atguigu.gulimall.member.web;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.member.feign.OrderFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Peter
 * @date 2022/5/24 0:54
 * @description Usage
 */
@Slf4j
@Controller
public class MemberWebController {

    @Autowired
    OrderFeignService orderFeignService;

    @GetMapping("/{page}.html")
    public String listPage(@PathVariable("page") String page) {

        return page;
    }

    @GetMapping("/memberOrder.html")
    public String memberOrderPage(@RequestParam(value = "pageNum",defaultValue = "1")Integer pageNum,
                                  Model model) {
        //查出当前登录用户的所有订单列表
        Map<String,Object> page = new HashMap<>();
        page.put("page", pageNum.toString());

        R r = orderFeignService.listWithItem(page);
        log.info(JSON.toJSONString(r));
        model.addAttribute("orders", r);

        return "orderList";
    }
}
