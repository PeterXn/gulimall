package com.atguigu.gulimall.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 使用rabbitmq
 * 1.引入spring-boot-starter-amqp；RabbitAutoConfiguration就会自动装配
 * 2.给容器中自动配置了：RabbitTemplate,AmqpAdmin,CachingConnectionFactory,RabbitMessagingTemplate
 * 3.使用AmqpAdmin；属性以prefix = "spring.rabbitmq"绑定；
 *   public class RabbitProperties
 * 4.@EnableRabbit 开启功能
 * 5.监听消息，使用@RabbitListener；必须有@EnableRabbit.
 *   @RabbitListener: 类+方法上（监听列队）
 *   @RabbitHandler: 标在方法（重载区分不同的消息）
 *
 *  本地事务失效：
 *  同一个对象内(同一个service内的事务)事务方法互调默认失效，原因：绕过了代理对象。
 *  解决：使用代理对象来调用事务方法
 *  ⑴、引入aop-starter;spring-boot-starter-aop;引入了aspectj
 *  ⑵、@EnableAspectjAutoProxy(exposeProxy=true)；开启动态代理
 */
@EnableRedisHttpSession
@EnableFeignClients
@EnableRabbit
@EnableDiscoveryClient
@SpringBootApplication
public class GulimallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallOrderApplication.class, args);
    }

}
