package com.atguigu.gulimall.seckill.scheduled;

import com.atguigu.gulimall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author Peter
 * @date 2022/6/1 17:18
 * @description 秒杀商品定时上架；每天晚上3点上架，上架最近3天需要秒杀的商品
 */
@Slf4j
@Service
public class SecKillSkuSchedule {

    @Autowired
    SeckillService seckillService;

    @Autowired
    RedissonClient redissonClient;

    private final String UPLOAD_LOCK = "seckill:upload:lock";

    /**
     * 每日3点上架秒杀商品
     * @Scheduled(cron = "0 0 3 * * ?")
     * //TODO 幂等性处理->redisson锁解决
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void uploadSecKillSkuLatest3Days() {
        log.info("上架秒杀商品信息...");

        RLock lock = redissonClient.getLock(UPLOAD_LOCK);
        lock.lock(20, TimeUnit.SECONDS);
        try {
            seckillService.uploadSeckillSkuLatest3Days();
        } finally {
            lock.unlock();
        }
    }
}
