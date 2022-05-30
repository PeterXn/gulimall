package com.atguigu.gulimall.order.vo;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * @author Peter
 * @date 2022/5/27 15:13
 * @description 封装订单提交的数据
 */
@Data
@ToString
public class OrderSubmitVo {

    /**
     * 收货地址id
     */
    private Long addrId;
    /**
     * 支付方式
     */
    private Integer payType;
    /**
     * 无须提交需要购买的商品，去购物车再去查询一遍
     */
    //优惠、发票

    /**
     * 防重令牌
     */
    private String orderToken;
    /**
     * 应付价格,验价
     */
    private BigDecimal payPrice;
    private String note;

    //用户相关信息，去session取出登录的信息

}
