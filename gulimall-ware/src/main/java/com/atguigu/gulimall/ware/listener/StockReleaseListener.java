package com.atguigu.gulimall.ware.listener;

import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.to.mq.StockLockedTo;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @author Peter
 * @date 2022/5/29 9:34
 * @description Usage
 */
@RabbitListener(queues = "stock.release.stock.queue")
@Service
public class StockReleaseListener {

    @Autowired
    WareSkuService wareSkuService;


    /**
     * 下订单成功，库存锁定成功但接下来的业务调用失败；导致订单回滚，之前锁定的库存也需要回滚。
     * <p>
     * 只要解锁库存的消息失败，一定要告知服务端解锁失败。（手动确认消息签收）
     */
    @RabbitHandler
    public void handlerStockLockedRelease(StockLockedTo to, Message message, Channel channel) throws IOException {
        System.out.println("收到解锁库存的消息: " + to);
        try {
            wareSkuService.unlockStock(to);

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
            e.printStackTrace();
        }

    }


    @RabbitHandler
    public void handlerOrderCloseRelease(OrderTo orderTo, Message message, Channel channel) throws IOException {
        System.out.println("收到订单关闭的消息: " + orderTo);
        try {
            wareSkuService.unlockStock(orderTo);

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
            e.printStackTrace();
        }

    }


}
