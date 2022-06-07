package com.atguigu.gulimall.order.listener;

import com.atguigu.common.to.mq.SeckillOrderTo;
import com.atguigu.gulimall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @author Peter
 * @date 2022/5/29 10:55
 * @description Usage
 */
@Slf4j
@Service
@RabbitListener(queues = "order.seckill.order.queue")
public class OrderSeckillListener {

    @Autowired
    OrderService orderService;

    @RabbitHandler
    public void orderListener(SeckillOrderTo seckillOrderTo, Channel channel, Message message) throws IOException {
        try {
            log.info("准备创建秒杀定单-->[{}]", seckillOrderTo.getOrderSn());
            orderService.createSeckillOrder(seckillOrderTo);
            //手动确认收到信息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }

}
