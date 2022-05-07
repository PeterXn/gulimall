package com.atguigu.gulimall.ware.vo;

import lombok.Data;
import sun.rmi.runtime.Log;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/5/7 0:41
 * @description description
 */
@Data
public class SkuHasStockVo {

    private Long skuId;
    private Boolean hasStock;
}
