package com.atguigu.gulimall.ware.vo;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Peter
 * @date 2022/5/24 10:49
 * @description Usage
 */
@Data
@ToString
public class OrderItemVo {

    private Long skuId;

    private String title;

    private String image;

    /**
     * 商品套餐属性
     */
    private List<String> skuAttrs;

    private BigDecimal price;

    private Integer count;

    private BigDecimal totalPrice;

    //TODO 查询库存
    private BigDecimal weight;
}
