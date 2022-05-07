package com.atguigu.common.to;

import lombok.Data;

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
