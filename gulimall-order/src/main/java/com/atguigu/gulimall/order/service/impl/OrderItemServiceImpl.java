package com.atguigu.gulimall.order.service.impl;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.order.dao.OrderItemDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@RabbitListener(queues = {"hello-java-queue"})
@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * queues:声明需要监听的所有队列
     * 可以很多人来监听。只要收到信息，队列就删除消息，而且只能有一个收到此消息。
     *
     * class org.springframework.amqp.core.Message
     * 1.Message msg：原生消息详细信息；头+体
     *
     *
     * @param msg org.springframework.amqp.core.Message
     * @param context 发送信息的类型；避免手动转换。OrderReturnReasonEntity context
     * @param channel 当前传输数据的通道
     *
     * //@RabbitListener(queues = {"hello-java-queue"})
     */
    @RabbitHandler
    public void receiveRabbitMsg(Message msg,
                                 OrderReturnReasonEntity context,
                                 Channel channel) {
        //Body:'{"id":1234,"name":"人之初","sort":null,"status":null,"createTime":1653293922421}'
        byte[] body = msg.getBody();

        System.out.println("OrderReturnReasonEntity..." + msg + "==>内容：" + context);

        long deliveryTag = msg.getMessageProperties().getDeliveryTag();

        //手动签到消息
        try {
            if (deliveryTag % 2 == 0) {
                //签收
                //boolean multiple:false,非批量模式；
                channel.basicAck(deliveryTag, false);
                System.out.println("消息已签收 === " + deliveryTag);
            } else {
                //拒签，退货
                //basicNack(long deliveryTag, boolean multiple, boolean requeue)
                //boolean requeue:true,发回服务器，重新入队；false,丢弃
                channel.basicNack(deliveryTag,false,false);
                //basicReject(long deliveryTag, boolean requeue)
                //channel.basicReject();
                System.out.println("消息没有签收 --- " + deliveryTag);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @RabbitHandler:方法重载
     */
    @RabbitHandler
    public void receiveRabbitMsg(Message msg,
                                 OrderEntity context,
                                 Channel channel) {
        //Body:'{"id":1234,"name":"人之初","sort":null,"status":null,"createTime":1653293922421}'
        byte[] body = msg.getBody();

        System.out.println("OrderEntity..." + msg + "==>内容：" + context);
    }

}