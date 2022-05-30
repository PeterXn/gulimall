package com.atguigu.common.to.mq;

import lombok.Data;
import lombok.ToString;

/**
 * @author Peter
 * @date 2022/5/29 3:15
 * @description Usage
 */
@Data
@ToString
public class StockDetailTo {

    private Long id;
    /**
     * sku_id
     */
    private Long skuId;
    /**
     * sku_name
     */
    private String skuName;
    /**
     * 购买个数
     */
    private Integer skuNum;
    /**
     * 工作单id
     */
    private Long taskId;

    /**
     * 仓库id
     */
    private Long wareId;

    /**
     * 锁定状态
     * 1-锁定 2-解锁 3-扣减
     */
    private Integer lockStatus;
}
