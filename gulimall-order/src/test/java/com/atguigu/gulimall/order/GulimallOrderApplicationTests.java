package com.atguigu.gulimall.order;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.UUID;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallOrderApplicationTests {

    @Autowired
    AmqpAdmin amqpAdmin;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Test
    public void testSendMessage() {
        String msg = "msg from spring boot!";
        for (int i = 0; i < 10; i++) {
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
                rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java",
                        orderEntity,new CorrelationData(UUID.randomUUID().toString()));
            }
        }
    }

    /**
     * 1.如何创建Exchange,Queue,Binding;
     * 使用AmqpAdmin；属性以prefix = "spring.rabbitmq"绑定；
     * public class RabbitProperties
     * 2.如何收发信息？
     */
    @Test
    public void createExchange() {
        //hello.java.exchange
        //public DirectExchange(String name, boolean durable, boolean autoDelete, Map<String, Object> arguments)
        DirectExchange directExchange = new DirectExchange("hello-java-exchange", true, false);
        amqpAdmin.declareExchange(directExchange);
        log.info("Exchange[{}]创建成功", directExchange.getName());
    }

    @Test
    public void createQueue() {
        //public Queue(String name, boolean durable, boolean exclusive, boolean autoDelete)
        Queue queue = new Queue("hello-java-queue", true, false, false);
        amqpAdmin.declareQueue(queue);
        log.info("Queue[{}]创建成功", queue.getName());
    }

    @Test
    public void createBinding() {
        //public Binding(String destination, DestinationType destinationType, String exchange, String routingKey,
        //			Map<String, Object> arguments)

        //将exchange与destinationType的destination进行绑定
        Binding binding = new Binding("hello-java-queue",
                Binding.DestinationType.QUEUE,
                "hello-java-exchange",
                "hello.java", null);
        amqpAdmin.declareBinding(binding);
        log.info("Binding[{}]创建成功", binding.getExchange());
    }

}
