package com.atguigu.gulimall.seckill.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @author Peter
 * @description 自定义系列化器
 * @date 2022/5/23 15:56
 */
@Configuration
public class MyRabbitConfig {


    /**
     * 使用JSON序列化机制，进行消息转换
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 定制RabbitTemplate
     * 1.服务收到就回调(只要消息抵达Broker)
     *    1.spring.rabbitmq.publisher-confirms=true
     *    2.设置确认回调 ConfirmCallback
     *
     * 2.消息正确抵达Queue进行回调
     *    # 开启消息抵达队列的确认
     *    spring.rabbitmq.publisher-returns=true
     *    # 只要抵达队列，以异步发送优先回调
     *    spring.rabbitmq.template.mandatory=true
     *    确认回调returnedMessage
     *
     * 3.消费端确认(保证每个消息被正常消费，此时才可以broker删除)
     *    spring.rabbitmq.listener.simple.acknowledge-mode=manual(手动签收)
     *    默认是自动确认的。
     *    ① 只要我们没有显式告诉MQ，消息被签收，没有ack。消息就一直是unacked状态.
     *    即使Consumer宕机，消息也不会丢失，会重新变为ready,下一次consumer重新连接，消息继续发送。
     *    ② 如何签收？
     *    channel.basicAck(deliveryTag, false);签到
     *    channel.basicNack(deliveryTag,false,false);拒签
     *
     */

}
