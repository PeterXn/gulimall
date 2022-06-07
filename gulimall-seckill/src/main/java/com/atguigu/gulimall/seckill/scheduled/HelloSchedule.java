package com.atguigu.gulimall.seckill.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;




/**
 * 定时任务与异步任务是独立。这两组注解可以分开使用。
 *
 * 定时任务
 *     1.@EnableScheduling 开启定时任务
 *     2.@Scheduled 开启一个定时任务
 *     3.自动配置类，TaskSchedulingAutoConfiguration
 *
 * 异步任务
 *     1.@EnableAsync 开户异步任务功能；
 *     2.@Async 在方法上添加
 *     3.自动配置类，TaskExecutionAutoConfiguration
 */
@Slf4j
@Component
//@EnableAsync 加在配置类中
//@EnableScheduling
public class HelloSchedule {

    /**
     * 1.spring中由6位组成，不支持第7位；
     * 2.在周的位置，1-7是代表周一到周日，或 MON-SUN;
     * 3.定时任务不应该阻塞；(默认是阻塞的)
     *   1)业务以异步编排；
     *   2)支持定时任务线程池；TaskSchedulingProperties(spring.task.scheduling)不好使
     *   3)让定时任务异步执行；
     *
     *
     *   使用异步任务+定时任务来实现定时任务不阻塞的功能
     */
    @Async
    @Scheduled(cron = "0 0/30 * * * ?")
    public void hello() throws InterruptedException {

        log.info("hello ...");
        Thread.sleep(3000);
    }

}
