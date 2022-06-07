package com.atguigu.common.to.mq;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * @author Peter
 * @date 2022/6/4 19:32
 * @description 秒杀定单
 */
@Data
@ToString
public class SeckillOrderTo {

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 活动场次id
     */
    private Long promotionSessionId;
    /**
     * 商品id
     */
    private Long skuId;
    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;

    /**
     * 购买数量
     */
    private Integer num;

    /**
     * 会员ID
     */
    private Long memberId;


}
