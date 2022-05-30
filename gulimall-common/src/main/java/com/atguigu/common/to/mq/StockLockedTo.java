package com.atguigu.common.to.mq;

import lombok.Data;
import lombok.ToString;

/**
 * @author Peter
 * @date 2022/5/29 3:08
 * @description Usage
 */
@Data
@ToString
public class StockLockedTo {

    /**
     * 库存工作单的id
     */
    private Long id;

    /** 库存单详情 wms_ware_order_task_detail**/
    private StockDetailTo detailTo;


}
