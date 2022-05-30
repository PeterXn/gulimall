package com.atguigu.gulimall.order.web;

import com.atguigu.gulimall.order.entity.OrderEntity;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.UUID;

/**
 * @author Peter
 * @date 2022/5/24 0:54
 * @description Usage
 */
@Controller
public class HelloController {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @GetMapping("/test/createOrder")
    @ResponseBody
    public String createOrder() {
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(UUID.randomUUID().toString());
        entity.setModifyTime(new Date());

        rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", entity);

        return "ok";
    }

    @GetMapping("/{page}.html")
    public String listPage(@PathVariable("page") String page) {

        return page;
    }
}
