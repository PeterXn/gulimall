package com.atguigu.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.mq.SeckillOrderTo;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.seckill.feign.CouponFeignService;
import com.atguigu.gulimall.seckill.feign.ProductFeignService;
import com.atguigu.gulimall.seckill.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.seckill.service.SeckillService;
import com.atguigu.gulimall.seckill.to.SeckillSkuRedisTo;
import com.atguigu.gulimall.seckill.vo.SeckillSessionWithSkus;
import com.atguigu.gulimall.seckill.vo.SkuInfoVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Peter
 * @date 2022/6/1 17:31
 * @description Usage
 */
@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    RedissonClient redissonClient;

    private final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";
    private final String SKUKILL_CACHE_PREFIX = "seckill:skus";
    /**
     * 秒杀库存信号量
     */
    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";

    /**
     * 秒杀用户标志位
     */
    private final String SECKILL_USER_FLAG = "seckill:user:";

    @Override
    public void uploadSeckillSkuLatest3Days() {
        //1.去数据库中查询参与秒杀的商品
        R session = couponFeignService.getLatest3DaysSession();
        if (session.getCode() == 0) {
            //上架商品
            List<SeckillSessionWithSkus> sessionData = session.getData(new TypeReference<List<SeckillSessionWithSkus>>() {
            });

            if (!CollectionUtils.isEmpty(sessionData)) {
                //缓存到redis中
                //1.缓存活动信息
                saveSessionInfos(sessionData);

                //2.缓存活动的关联商品信息
                saveSessionSkuInfos(sessionData);
            }
        }
    }

    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {

        //1.确定当前时间是属于哪个秒杀场次
        //long curr = new Date().getTime();
        long curr = System.currentTimeMillis();

        Set<String> keys = redisTemplate.keys(SESSIONS_CACHE_PREFIX + "*");
        for (String key : keys) {
            //seckill:sessions:1654171200000_1654176030000
            String replace = key.replace(SESSIONS_CACHE_PREFIX, "");
            String[] s = replace.split("_");
            Long startTime = Long.parseLong(s[0]);
            Long endTime = Long.parseLong(s[1]);
            if (curr >= startTime && curr <= endTime) {
                //2.获取当前场次的商品信息
                List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                // range: 4_5
                BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                List<String> list = hashOps.multiGet(range);
                if (!CollectionUtils.isEmpty(list)) {
                    List<SeckillSkuRedisTo> collect = list.stream().map(item -> {
                        SeckillSkuRedisTo redisTo = JSON.parseObject((String) item, SeckillSkuRedisTo.class);
                        //过滤随机码
                        redisTo.setRandomCode(null);

                        return redisTo;
                    }).collect(Collectors.toList());

                    return collect;
                }

                break;
            }
        }

        return null;
    }

    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {

        //找到所有参与秒杀商品的key
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);

        //同一个商品在同一时段只能参与一个秒杀
        Set<String> keys = hashOps.keys();
        if (!CollectionUtils.isEmpty(keys)) {
            String regx = "\\d_" + skuId;
            for (String key : keys) {
                // key: 4_5
                if (Pattern.matches(regx, key)) {
                    String json = hashOps.get(key);
                    SeckillSkuRedisTo redisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);
                    //随机码
                    long currTime = System.currentTimeMillis();
                    //现在正不是秒杀时段，清空token
                    if (currTime <= redisTo.getStartTime() || currTime >= redisTo.getEndTime()) {
                        redisTo.setRandomCode(null);
                    }

                    return redisTo;
                }

            }
        }

        return null;
    }

    /**
     * 处理秒杀请求
     *
     * @param killId
     * @param key
     * @param num
     * @return
     */
    @Override
    public String kill(String killId, String key, Integer num) {
        //http://seckill.gulimall.com/kill?killId=2_1&key=da120fd6c6dd4077985775d9ff79b0dd&num=1
        long l1 = System.currentTimeMillis();
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();

        //1.获取当前商品的详细信息
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);

        //redis中是否存在这个商品
        String json = hashOps.get(killId);
        if (StringUtils.isEmpty(json)) {
            //不存在
            log.info("秒杀的商品[{}]不存在", killId);
            return null;
        }

        //检验合法性
        SeckillSkuRedisTo redisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);

        //1.是否超过秒杀时间段
        Long startTime = redisTo.getStartTime();
        Long endTime = redisTo.getEndTime();
        long curr = System.currentTimeMillis();

        //设置秒杀成功的过期时间
        long ttl = endTime - curr;
        if (curr < startTime || curr > endTime) {
            //已过秒杀时段
            log.info("当前时间已超过秒杀时段");
            return null;
        }

        //2.校验随机码与商品id
        String randomCode = redisTo.getRandomCode();
        String redisKillId = redisTo.getPromotionSessionId() + "_" + redisTo.getSkuId();
        if (!(key.equals(randomCode) && killId.equals(redisKillId))) {
            log.info("检验码[{}]或商品id[{}]不正确", randomCode, killId);
            return null;
        }

        //3.验证购物数量
        if (num > redisTo.getSeckillLimit().intValue()) {
            log.info("当前秒杀数量[{}]已超限", num);
            return null;
        }

        //4.判断这个用户是否参与秒杀-->幂等性处理
        //只要秒杀成功，就去占位，userId_SessionId_skuId
        String redisKey = SECKILL_USER_FLAG + memberRespVo.getId() + "_" + redisKillId;
        //自动过期
        Boolean ifExist = redisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
        //ifExist:true->没有秒杀过；false->已参与过秒杀
        if (!ifExist) {
            log.info("当前用户[{}]已参与过秒杀", memberRespVo.getId());
            return null;
        }

        //5.信号量；获取信号量，是否还有秒杀库存
        RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);

        //boolean b = semaphore.tryAcquire(num, 100, TimeUnit.MILLISECONDS);
        boolean b = semaphore.tryAcquire(num);
        if (!b) {
            log.info("秒杀数量不足，请下次再试");
            return null;
        }

        //快速下单到mq
        String timeId = IdWorker.getTimeId();
        SeckillOrderTo orderTo = new SeckillOrderTo();
        orderTo.setOrderSn(timeId);
        orderTo.setMemberId(memberRespVo.getId());
        orderTo.setNum(num);
        orderTo.setPromotionSessionId(redisTo.getPromotionSessionId());
        orderTo.setSkuId(redisTo.getSkuId());
        orderTo.setSeckillPrice(redisTo.getSeckillPrice());

        rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", orderTo);

        log.info("订单[{}]已发往定单系统", orderTo);
        long l2 = System.currentTimeMillis();
        log.info("单个订单耗时[{}]毫秒", (l2 - l1));

        return timeId;

    }

    private void saveSessionInfos(List<SeckillSessionWithSkus> sessions) {
        sessions.stream().forEach(session -> {
            long startTime = session.getStartTime().getTime();
            long endTime = session.getEndTime().getTime();
            String key = SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;
            Boolean hasKey = redisTemplate.hasKey(key);
            //不存在才执行保存
            if (!hasKey) {
                List<String> collect = session.getRelationSkus().stream()
                        .map(item -> item.getPromotionSessionId().toString() + "_" + item.getSkuId().toString())
                        .collect(Collectors.toList());
                //缓存活动信息
                redisTemplate.opsForList().leftPushAll(key, collect);
            }
        });
    }

    private void saveSessionSkuInfos(List<SeckillSessionWithSkus> sessions) {

        sessions.stream().forEach(session -> {
            //准备hash操作
            BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
            session.getRelationSkus().stream().forEach(seckillSkuVo -> {
                //4.商品的随机码
                String token = UUID.randomUUID().toString().replace("-", "");
                String skuKey = seckillSkuVo.getPromotionSessionId().toString() + "_" + seckillSkuVo.getSkuId().toString();
                if (!ops.hasKey(skuKey)) {
                    //缓存商品
                    SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                    //1.sku的基本信息
                    R skuInfo = productFeignService.getSkuInfo(seckillSkuVo.getSkuId());
                    if (skuInfo.getCode() == 0) {
                        SkuInfoVo skuInfoVo = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        redisTo.setSkuInfo(skuInfoVo);
                    }

                    //2.sku的秒杀信息
                    BeanUtils.copyProperties(seckillSkuVo, redisTo);

                    //3.设置上架商品的秒杀开始与结束时间
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());

                    redisTo.setRandomCode(token);

                    //一个商品可能在一天的不同场次中上架，key须做区分
                    String jsonString = JSON.toJSONString(redisTo);
                    ops.put(skuKey, jsonString);

                    //5.使用秒杀库存作为分布式的信号量-->限流
                    //设置信号量（库存量）
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    //商品的秒杀量作为信号量
                    semaphore.trySetPermits(seckillSkuVo.getSeckillCount().intValue());
                }
            });
        });
    }
}
