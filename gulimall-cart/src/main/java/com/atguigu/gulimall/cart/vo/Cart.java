package com.atguigu.gulimall.cart.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 当使用lombok时，部分get、set方法需要自定义，lombok不会再生成对应的方法。
 * 所以自己写get、set方法和lombok生成的方法不冲突。
 *
 * @author Peter
 * @date 2022/5/22 0:53
 * @description 整个购物车
 */
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Cart {

    /**
     * 购物项集合
     */
    private List<CartItem> items;
    /**
     * 商品件数（汇总购物车内商品总件数）
     */
    private Integer countNum;
    /**
     * 商品数量（汇总购物车内商品总个数）
     */
    private Integer countType;
    /**
     * 商品总价
     */
    private BigDecimal totalAmount;
    /**
     * 减免价格
     */
    private BigDecimal reduce = new BigDecimal("0.00");


    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public Integer getCountNum() {
        int count = 0;
        if (items != null && items.size() > 0) {
            for (CartItem item : items) {
                count += item.getCount();
            }
        }
        return count;
    }

    public void setCountNum(Integer countNum) {
        this.countNum = countNum;
    }

    public Integer getCountType() {
        int count = 0;
        if (items != null && items.size() > 0) {
            for (CartItem item : items) {
                count += 1;
            }
        }
        return count;
    }

    public void setCountType(Integer countType) {
        this.countType = countType;
    }

    public BigDecimal getTotalAmount() {
        BigDecimal amount = new BigDecimal("0");
        // 计算购物项总价
        if (!CollectionUtils.isEmpty(items)) {
            for (CartItem cartItem : items) {
                if (cartItem.getCheck()) {
                    amount = amount.add(cartItem.getTotalPrice());
                }
            }
        }
        // 计算优惠后的价格
        return amount.subtract(getReduce());
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getReduce() {
        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }
}
