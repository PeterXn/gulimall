package com.atguigu.gulimall.order.to;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Peter
 * @date 2022/5/27 16:37
 * @description Usage
 */
@Data
@ToString
public class OrderCreateTo {

    private OrderEntity order;

    private List<OrderItemEntity> orderItems;

    /**
     * 订单计算的应付价格
     */
    private BigDecimal payPrice;

    /**
     * 运费
     */
    private BigDecimal fare;


}
