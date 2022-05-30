package com.atguigu.gulimall.ware.vo;

import com.sun.org.apache.xpath.internal.operations.Bool;
import lombok.Data;
import lombok.ToString;

/**
 * @author Peter
 * @date 2022/5/27 22:29
 * @description Usage
 */
@Data
@ToString
public class LockStockResult {

    private Long skuId;
    private Integer num;
    private Boolean locked;

}
