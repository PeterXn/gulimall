package com.atguigu.gulimall.order.controller;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.UUID;

/**
 * @description Usage
 * @date 2022/5/23 17:01
 * @author Peter
 */
@RestController
public class RabbitController {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @GetMapping("/sendMq")
    public String sendMq() {
        int num = 10;
        for (int i = 0; i < num; i++) {
            if (i % 2 == 0) {
                OrderReturnReasonEntity entity = new OrderReturnReasonEntity();
                entity.setId(Long.parseLong(i + ""));
                entity.setName("人之初");
                entity.setCreateTime(new Date());
                //发送的对象须实现Serializable序列化接口，
                rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java",
                        entity,new CorrelationData(UUID.randomUUID().toString()));
                //log.info("消息[{}]发送完成。", entity);
            } else {
                OrderEntity orderEntity = new OrderEntity();
                orderEntity.setOrderSn(UUID.randomUUID().toString());
                rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java-error",
                        orderEntity,new CorrelationData(UUID.randomUUID().toString()));
            }
        }

        return "ok";
    }
}
