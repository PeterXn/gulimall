package com.atguigu.gulimall.ware.vo;

import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * @author Peter
 * @date 2022/5/27 22:22
 * @description Usage
 */
@Data
@ToString
public class WareSkuLockVo {

    private String orderSn;

    /**
     * 需要锁定的库存信息
     */
    private List<OrderItemVo> locks;

}
