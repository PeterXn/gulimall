package com.atguigu.gulimall.cart.vo;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Peter
 * @date 2022/5/22 0:51
 * @description 购物项内容
 */
@ToString
@Data
public class CartItem {

    private Long skuId;

    private Boolean check = true;

    private String title;

    private String image;

    /**
     * 商品套餐属性
     */
    private List<String> skuAttrs;

    private BigDecimal price;

    private Integer count;

    private BigDecimal totalPrice;


    /**
     * 计算当前购物项总价
     * @return
     */
    public BigDecimal getTotalPrice() {

        return this.price.multiply(new BigDecimal("" + this.count));
    }

}
