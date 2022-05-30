package com.atguigu.gulimall.order.vo;

import lombok.Data;
import lombok.ToString;

/**
 * @author Peter
 * @date 2022/5/25 11:20
 * @description Usage
 */
@Data
@ToString
public class SkuStockVo {

    private Long skuId;
    private Boolean hasStock;
}
