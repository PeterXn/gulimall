package com.atguigu.gulimall.order.vo;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author Peter
 * @date 2022/5/24 10:24
 * @description 订单确认页需要的数据
 */
@Data
@ToString
public class OrderConfirmVo {

    /**
     * 收货地址， ums_member_receive_address 表
     */
    List<MemberAddressVo> address;

    /**
     * 所有选中的购物项
     */
    List<OrderItemVo> items;

    /**
     * 发票记录。。。
     */

    /**
     * 优惠劵
     */
    Integer integration;

    /**
     * 订单总额
     */
    BigDecimal total;

    /**
     * 应付总额
     */
    BigDecimal payPrice;

    /**
     * 防止重复提交令牌
     */
    String orderToken;

    Integer totalCount;

    Map<Long,Boolean> stocks;

    public BigDecimal getTotal() {
        BigDecimal sum = BigDecimal.ZERO;
        if (items != null) {
            for (OrderItemVo item : items) {
                BigDecimal multiply = item.getPrice().multiply(BigDecimal.valueOf(item.getCount()));
                sum = sum.add(multiply);
            }
        }

        return sum;
    }

    public BigDecimal getPayPrice() {
        return getTotal();
    }

    public Integer getTotalCount() {
        Integer count = 0;
        if (items != null && items.size() > 0) {
            for (OrderItemVo item : items) {
                count += item.getCount();
            }
        }
        return count;
    }
}
