package com.atguigu.gulimall.product.vo;

import lombok.Data;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/5/16 20:18
 * @description Usage
 */
@Data
public class SkuItemSaleAttrVo {

    private Long attrId;
    private String attrName;
    private List<AttrValueWithSkuIdVo> attrValues;
}
