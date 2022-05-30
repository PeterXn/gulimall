package com.atguigu.common.exception;

import lombok.Data;
import lombok.ToString;

/**
 * @author Peter
 * @date 2022/5/27 23:21
 * @description Usage
 */
@Data
@ToString
public class NoStockException extends RuntimeException{

    private Long skuId;

    public NoStockException(Long skuId) {
        super("商品id："+ skuId + "；库存不足！");
    }

    public NoStockException(String msg) {
        super(msg);
    }
}
