package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.OrderConstant;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.enume.OrderStatusEnum;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.ProductFeignService;
import com.atguigu.gulimall.order.feign.WareFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.to.OrderCreateTo;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private ThreadLocal<OrderSubmitVo> orderSubmitVoThreadLocal = new ThreadLocal<>();

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     * 线程池
     */
    @Autowired
    ThreadPoolExecutor executor;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * feign在远程调用会丢失请求头问题==>自定义RequestInterceptor解决
     * feign在异步情况下丢失上下文问题？
     */
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        /*
        feign在远程调用会丢失请求头问题==>自定义RequestInterceptor解决。
        feign在远程调用之前要构造请求，调用很多的拦截器(RequestInterceptor)
        for (RequestInterceptor interceptor : requestInterceptors)
         */
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();

        System.out.println("主线程..." + Thread.currentThread().getName());
        /*
        feign在异步情况下丢失上下文问题
         */
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();


        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            //1.远程查询所有的收货列表
            //feign在异步情况下丢失上下文问题
            RequestContextHolder.setRequestAttributes(requestAttributes);
            System.out.println("member线程..." + Thread.currentThread().getName());
            List<MemberAddressVo> address = memberFeignService.getAddress(memberRespVo.getId());
            confirmVo.setAddress(address);
        }, executor);

        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            //2.远程查询购物车中所有选中的购物项
            //feign在异步情况下丢失上下文问题
            RequestContextHolder.setRequestAttributes(requestAttributes);
            System.out.println("cart线程..." + Thread.currentThread().getName());
            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItems(items);
        }, executor).thenRunAsync(() -> {
            //查询库存信息
            List<OrderItemVo> items = confirmVo.getItems();
            //List<Long> collect = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            List<Long> collect = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());

            R hasStock = wareFeignService.getSkusHasStock(collect);
            List<SkuStockVo> data = hasStock.getData(new TypeReference<List<SkuStockVo>>() {
            });
            //转成map
            if (data != null) {
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(map);
            }
        }, executor);


        //3.查询用户积分
        Integer integration = memberRespVo.getIntegration();
        confirmVo.setIntegration(integration);

        //4.其他数据自动计算

        //TODO 5.防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        //redis存一个,30分钟过期
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId(), token, 30, TimeUnit.MINUTES);
        confirmVo.setOrderToken(token);

        //等待2个异步任务完成
        CompletableFuture.allOf(getAddressFuture, cartFuture).get();

        return confirmVo;
    }

    /**
     * @Transactional:本地事务。
     * 分布式事务如何回滚？
     * @param vo
     * @return
     */
    @Transactional(rollbackFor = RuntimeException.class)
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        SubmitOrderResponseVo response = new SubmitOrderResponseVo();

        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        orderSubmitVoThreadLocal.set(vo);

        //没有原子性操作
        //String redisToken = redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId());
        //if (StringUtils.isNotEmpty(orderToken) && StringUtils.isNotEmpty(redisToken) && orderToken.equals(redisToken)) {
        //    //令牌验证通过
        //} else {
        //    //信息验证失败
        //}
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //1.验证令牌[令牌的对比与删除必须原子性]
        String orderToken = vo.getOrderToken();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        String key = OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId();
        if (StringUtils.isEmpty(orderToken)) {
            //页面传递的token为空
            response.setCode(1);
            return response;
        } else {
            //令牌的对比与删除是原子性
            Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(key), orderToken);
            //0,令牌失败，1，令牌删除成功
            if (result == 0L) {
                //失败
                response.setCode(1);
                return response;
            } else {
                //令牌验证成功
                //下单：去创建订单、验价格、锁库存
                //1.创建订单、订单项信息
                OrderCreateTo orderCreateTo = createOrder();

                //2.验证页面的价格与后台计算的价格,
                BigDecimal payAmount = orderCreateTo.getOrder().getPayAmount();
                //页面的价格
                BigDecimal payPrice = vo.getPayPrice();
                //两者的价格相差0.01内视为一样
                if (Math.abs(payAmount.subtract(payPrice).doubleValue()) > 0.01) {
                    //验价超限
                    response.setCode(2);
                    return response;
                } else {
                    //验价通过；继续;
                    //3.保存订单
                    saveOrder(orderCreateTo);

                    //4.锁定库存；只要有异常就回滚订单数据
                    //订单号、所有订单项（skuId,skuName,num）
                    WareSkuLockVo lockVo = new WareSkuLockVo();
                    lockVo.setOrderSn(orderCreateTo.getOrder().getOrderSn());

                    List<OrderItemVo> locks = orderCreateTo.getOrderItems().stream().map(item -> {
                        OrderItemVo itemVo = new OrderItemVo();
                        itemVo.setSkuId(item.getSkuId());
                        itemVo.setCount(item.getSkuQuantity());
                        itemVo.setTitle(item.getSkuName());

                        return itemVo;
                    }).collect(Collectors.toList());
                    lockVo.setLocks(locks);
                    //远程锁定库存
                    //库存成功了但是网络原因超时了，订单回滚，库存不回滚。
                    R r = wareFeignService.orderLockStock(lockVo);
                    if (r.getCode() != 0) {
                        //锁定失败
                        response.setCode(3);
                        String msg = (String) r.get("msg");
                        throw new NoStockException(msg);
                    } else {
                        //锁定成功
                        response.setOrder(orderCreateTo.getOrder());
                        //int i = 1 / 0;
                        //TODO 下单成功，则发信息到定单延时队列中，30mim后查询订单状态再做处理
                        rabbitTemplate.convertAndSend("order-event-exchange",
                                "order.create.order",
                                orderCreateTo.getOrder());

                        return response;
                    }

                }

            }
        }

    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        OrderEntity orderEntity = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));

        return orderEntity;
    }

    @Override
    public void closeOrder(OrderEntity orderEntity) {
        //查询订单的最新状态
        OrderEntity byId = this.getById(orderEntity.getId());
        //关单的条件
        if (byId.getStatus().equals(OrderStatusEnum.CREATE_NEW.getCode())) {
            OrderEntity update = new OrderEntity();
            update.setId(byId.getId());
            update.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(update);

            //关闭订单主动发库存queue
            //byId的定单状态不是最新?不影响，库存解锁会从数据库再查一次
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(byId, orderTo);
            orderTo.setStatus(update.getStatus());
            rabbitTemplate.convertAndSend("order-event-exchange","order.release.other",orderTo);
        }
    }

    /**
     * 保存订单数据
     * @param orderCreateTo
     */
    private void saveOrder(OrderCreateTo orderCreateTo) {
        OrderEntity orderEntity = orderCreateTo.getOrder();
        this.save(orderEntity);

        List<OrderItemEntity> orderItems = orderCreateTo.getOrderItems();
        orderItemService.saveBatch(orderItems);

    }


    private OrderCreateTo createOrder() {
        OrderCreateTo createTo = new OrderCreateTo();

        //1.订单号 生成订单号
        String orderSn = IdWorker.getTimeId();
        OrderEntity orderEntity = buildOrder(orderSn);


        //2.获取到所有的订单项
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderSn);

        //3.计算价格、积分相关
        computePrice(orderEntity, orderItemEntities);

        createTo.setOrder(orderEntity);
        createTo.setOrderItems(orderItemEntities);

        return createTo;
    }

    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal coupon = BigDecimal.ZERO;
        BigDecimal integration = BigDecimal.ZERO;
        BigDecimal promotion = BigDecimal.ZERO;

        Integer gift = 0;
        Integer growth = 0;

        //订单的总额,叠加每一个订单项的总额信息
        for (OrderItemEntity entity : orderItemEntities) {
            coupon = coupon.add(entity.getCouponAmount());
            integration = integration.add(entity.getIntegrationAmount());
            promotion = promotion.add(entity.getPromotionAmount());
            total = total.add(entity.getRealAmount());
            gift = gift + entity.getGiftIntegration();
            growth = growth + entity.getGiftGrowth();
        }
        //订单相关价格
        orderEntity.setTotalAmount(total);
        //应付总额
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));

        orderEntity.setCouponAmount(coupon);
        orderEntity.setIntegrationAmount(integration);
        orderEntity.setPromotionAmount(promotion);

        //设置积分
        orderEntity.setIntegration(gift);
        orderEntity.setGrowth(growth);

        //删除状态，0=未删除
        orderEntity.setDeleteStatus(0);

    }

    private OrderEntity buildOrder(String orderSn) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);

        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        entity.setMemberId(memberRespVo.getId());

        OrderSubmitVo submitVo = orderSubmitVoThreadLocal.get();
        //收货信息
        R r = wareFeignService.getFare(submitVo.getAddrId());
        FareVo fareResp = r.getData(new TypeReference<FareVo>() {
        });
        //运费
        entity.setFreightAmount(fareResp.getFare());
        //设置收货人信息
        entity.setReceiverCity(fareResp.getAddress().getCity());
        entity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
        entity.setReceiverName(fareResp.getAddress().getName());
        entity.setReceiverPhone(fareResp.getAddress().getPhone());
        entity.setReceiverPostCode(fareResp.getAddress().getPostCode());
        entity.setReceiverProvince(fareResp.getAddress().getProvince());
        entity.setReceiverRegion(fareResp.getAddress().getRegion());

        //设置订单的相关状态
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        entity.setModifyTime(new Date());

        return entity;
    }

    /**
     * 构造所有订单项数据
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        //最后一次从数据库确定每个购物项的价格
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if (!CollectionUtils.isEmpty(currentUserCartItems)) {
            List<OrderItemEntity> orderItemEntities = currentUserCartItems.stream().map(cartItem -> {
                OrderItemEntity itemEntity = buildOrderItem(cartItem);
                itemEntity.setOrderSn(orderSn);

                return itemEntity;
            }).collect(Collectors.toList());

            return orderItemEntities;
        }
        return null;
    }

    /**
     * 构建某一个订单项
     */
    private OrderItemEntity buildOrderItem(OrderItemVo orderItemVo) {
        OrderItemEntity itemEntity = new OrderItemEntity();
        //1.订单号
        //2.商品的spu信息
        Long skuId = orderItemVo.getSkuId();
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo data = r.getData(new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(data.getId());
        itemEntity.setSpuBrand(data.getBrandId().toString());
        itemEntity.setSpuName(data.getSpuName());
        itemEntity.setCategoryId(data.getCatalogId());

        //3.商品的sku信息
        itemEntity.setSkuId(orderItemVo.getSkuId());
        itemEntity.setSkuName(orderItemVo.getTitle());
        itemEntity.setSkuPic(orderItemVo.getImage());
        itemEntity.setSkuPrice(orderItemVo.getPrice());
        String skuAttrs = org.springframework.util.StringUtils.collectionToDelimitedString(orderItemVo.getSkuAttrs(), "；");
        itemEntity.setSkuAttrsVals(skuAttrs);
        itemEntity.setSkuQuantity(orderItemVo.getCount());

        //4.商品的优惠信息(不做)
        //5.商品的积分信息
        itemEntity.setGiftGrowth(orderItemVo.getPrice().multiply(new BigDecimal(orderItemVo.getCount())).intValue());
        itemEntity.setGiftIntegration(orderItemVo.getPrice().multiply(new BigDecimal(orderItemVo.getCount())).intValue());

        //6.订单项的价格
        itemEntity.setPromotionAmount(BigDecimal.ZERO);
        itemEntity.setCouponAmount(BigDecimal.ZERO);
        itemEntity.setIntegrationAmount(BigDecimal.ZERO);
        //当前订单项的实际金额. 总额-各种优惠
        BigDecimal originPrice = itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity()));
        BigDecimal realPrice = originPrice.subtract(itemEntity.getPromotionAmount())
                .subtract(itemEntity.getCouponAmount())
                .subtract(itemEntity.getIntegrationAmount());
        itemEntity.setRealAmount(realPrice);


        return itemEntity;
    }

}