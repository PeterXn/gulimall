package com.atguigu.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.enume.OrderStatusEnum;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.to.mq.StockDetailTo;
import com.atguigu.common.to.mq.StockLockedTo;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.dao.WareSkuDao;
import com.atguigu.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.atguigu.gulimall.ware.entity.WareOrderTaskEntity;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.feign.OrderFeignService;
import com.atguigu.gulimall.ware.feign.ProductFeignService;
import com.atguigu.gulimall.ware.service.WareOrderTaskDetailService;
import com.atguigu.gulimall.ware.service.WareOrderTaskService;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.atguigu.gulimall.ware.vo.OrderItemVo;
import com.atguigu.gulimall.ware.vo.OrderVo;
import com.atguigu.gulimall.ware.vo.SkuHasStockVo;
import com.atguigu.gulimall.ware.vo.WareSkuLockVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    WareSkuDao wareSkuDao;

    /**
     * 远程调用接口
     */
    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    OrderFeignService orderFeignService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    WareOrderTaskService wareOrderTaskService;

    @Autowired
    WareOrderTaskDetailService wareOrderTaskDetailService;

    /**
     * 下订单成功，库存锁定成功但接下来的业务调用失败；导致订单回滚，之前锁定的库存也需要回滚。
     * <p>
     * 只要解锁库存的消息失败，一定要告知服务端解锁失败。（手动确认消息签收）
     */

    private void unLockStock(Long skuId, Long wareId, Integer skuNum, Long detailId) {
        //库存解锁
        wareSkuDao.unLockStock(skuId, wareId, skuNum);
        //更新工作单状态
        WareOrderTaskDetailEntity detailEntity = new WareOrderTaskDetailEntity();
        detailEntity.setId(detailId);
        //1-已锁定；2-已解锁；3-库存扣减
        detailEntity.setLockStatus(2);
        wareOrderTaskDetailService.updateById(detailEntity);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        /*
        skuId:
        wareId:
        id  sku_id  ware_id   stock  sku_name  stock_locked
         */
        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)) {
            queryWrapper.eq("sku_id", skuId);
        }

        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId)) {
            queryWrapper.eq("ware_id", wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        // 判断如果还没有这个库存记录，就新增
        List<WareSkuEntity> entities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if (entities == null || entities.size() == 0) {
            // 新增记录
            WareSkuEntity skuEntity = new WareSkuEntity();
            skuEntity.setSkuId(skuId);
            skuEntity.setStock(skuNum);
            skuEntity.setWareId(wareId);
            skuEntity.setStockLocked(0);
            /*
            远程查询sku的名字
            1. 用try/catch异常
            2. TODO 还可以用什么办法让异常出现以后不回滚？高级部分解密
             */
            try {
                R info = productFeignService.info(skuId);
                Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");
                if (info.getCode() == 0) {
                    skuEntity.setSkuName((String) data.get("skuName"));
                }
            } catch (Exception e) {
                log.error("调用product系统异常：[{}]", e.getCause());
            }
            wareSkuDao.insert(skuEntity);
        } else {
            // 更新记录
            wareSkuDao.addStock(skuId, wareId, skuNum);
        }
    }

    @Override
    public List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds) {

        List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockVo vo = new SkuHasStockVo();

            // 查询当前sku的总库存
            Long count = baseMapper.getSkuStock(skuId);
            vo.setSkuId(skuId);
            //vo.setHasStock(count == null ? false : count > 0);
            vo.setHasStock(count != null && count > 0);
            return vo;
        }).collect(Collectors.toList());

        return collect;
    }

    /**
     * 为某个订单锁定库存
     * <p>
     * 库存解锁的场景：
     * 1.下订单成功、订单过期没有支付、用户手动取消订单；都要解锁库存；
     * <p>
     * 2.下订单成功，库存锁定成功但接下来的业务调用失败；导致订单回滚，之前锁定的库存也需要回滚。
     */
    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public boolean orderLockStock(WareSkuLockVo vo) {
        /**
         * 保存库存工作单，便于回滚恢复。
         */
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
        taskEntity.setCreateTime(new Date());
        wareOrderTaskService.save(taskEntity);


        //1.按照下单的收货地址，找到一个就近仓库，锁定库存
        //1.找到每个商品在哪个仓库都有库存
        List<OrderItemVo> locks = vo.getLocks();
        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            //查询这个商品在哪里都有库存
            List<Long> wareIds = wareSkuDao.listWareIdHasSkuStock(skuId);
            stock.setWareIds(wareIds);
            return stock;
        }).collect(Collectors.toList());

        //2.锁定仓库
        for (SkuWareHasStock hasStock : collect) {
            Boolean skuStocked = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareIds();
            if (CollectionUtils.isEmpty(wareIds)) {
                //没有任何仓库有这个商品的库存
                throw new NoStockException(skuId);
            }
            for (Long wareId : wareIds) {
                Long count = wareSkuDao.lockSkuStock(skuId, wareId, hasStock.getNum());
                if (count == 1) {
                    //当前商品在此仓库锁定成功
                    skuStocked = true;
                    //保存库存工作单详情
                    WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity(null, skuId, null, hasStock.getNum(), taskEntity.getId(), wareId, 1);
                    wareOrderTaskDetailService.save(entity);
                    //TODO 告诉MQ库存锁定成功
                    StockLockedTo lockedTo = new StockLockedTo();
                    lockedTo.setId(taskEntity.getId());
                    // 创建库存锁定工作单消息（每一件商品一条消息）
                    StockDetailTo detailTo = new StockDetailTo();
                    BeanUtils.copyProperties(entity, detailTo);
                    lockedTo.setDetailTo(detailTo);

                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", lockedTo);
                    break;
                } else {
                    //当前仓库锁定失败，重试下一个仓库
                }
            }
            if (!skuStocked) {
                //当前商品在所有仓库都锁定失败
                throw new NoStockException(skuId);
            }
        }

        //3.运行到这里都是库存锁定成功
        return true;
    }

    @Override
    public void unlockStock(StockLockedTo to) {

        StockDetailTo detail = to.getDetailTo();
        Long detailId = detail.getId();
        //查询数据库这个订单的锁定库信息，有记录则回滚。
        //没有记录，说明库存锁定失败，库存本身回滚了，这个情况无须解锁。
        WareOrderTaskDetailEntity byId = wareOrderTaskDetailService.getById(detailId);
        if (byId != null) {
            //解锁
            Long id = to.getId();
            WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();
            //去订单系统查询这订单的状态
            //feign.codec.DecodeException: Could not extract response: no suitable HttpMessageConverter found for response type
            //[class com.atguigu.common.utils.R] and content type [text/html;charset=UTF-8]
            R r = orderFeignService.getOrderStatus(orderSn);
            if (r.getCode() == 0) {
                OrderVo orderVo = r.getData(new TypeReference<OrderVo>() {
                });
                if (orderVo == null || orderVo.getStatus().equals(OrderStatusEnum.CANCLED.getCode())) {
                    //订单已被取消，才能解锁库存
                    //工作单中的锁定状态：1-已锁定；2-已解锁；3-库存扣减
                    if (byId.getLockStatus() == 1) {
                        unLockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detailId);
                    }
                }
            } else {
                //消息被拒以后重新放到队列里面，让别人继续消费解锁
                throw new RuntimeException("order远程服务失败");
            }

        } else {
            //无须解锁
        }

    }

    /**
     * 防止订单服务异常，订单的状态消息一直无法修改。库存优先到期。
     * 查询订单状态为新建，导致库存无法解锁。
     */
    @Transactional(rollbackFor = RuntimeException.class)
    @Override
    public void unlockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        //查订单最新的状态

        //查询库存的最新状态
        WareOrderTaskEntity taskEntity = wareOrderTaskService.getOrderTaskByOrderSn(orderSn);
        Long id = taskEntity.getId();
        //按照工作单id，和没解锁的状态查询
        //1-已锁定；2-已解锁；3-库存扣减
        List<WareOrderTaskDetailEntity> taskDetailEntities = wareOrderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>()
                .eq("task_id", id)
                .eq("lock_status", 1));
        for (WareOrderTaskDetailEntity entity : taskDetailEntities) {
            //Long skuId, Long wareId, Integer skuNum, Long detailId
            unLockStock(entity.getSkuId(),entity.getWareId(),entity.getSkuNum(), entity.getId());
        }
    }

    @Data
    class SkuWareHasStock {
        private Long skuId;
        private Integer num;
        private List<Long> wareIds;
    }
}