package com.atguigu.gulimall.order.web;

import com.alipay.api.AlipayApiException;
import com.atguigu.gulimall.order.config.AlipayTemplate;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Peter
 * @date 2022/5/30 17:55
 * @description Usage
 */
@Controller
public class PayWebController {

    @Autowired
    AlipayTemplate alipayTemplate;

    @Autowired
    OrderService orderService;

    @ResponseBody
    @GetMapping(value="/payOrder",produces = "text/html")
    public String payOrder(@RequestParam("orderSn") String orderSn){
        System.out.println("orderSn = " + orderSn);

        PayVo payVo = orderService.getOrderPay(orderSn);

        String pay = null;
        try {
            pay = alipayTemplate.pay(payVo);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        System.out.println("pay = " + pay);

        return pay;
    }
}
