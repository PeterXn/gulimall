package com.atguigu.gulimall.product.vo;

import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/5/16 16:02
 * @description Item详情
 */
@Data
public class SkuItemVo {

    /**
     * // 1.sku基本信息获取 pms_sku_info
     */
    SkuInfoEntity info;

    /**
     * 是否有货
     */
    boolean hasStock = true;

    /**
     * // 2.sku的图片信息 pms_sku_images
     */
    List<SkuImagesEntity> images;

    /**
     * // 3.获取spu的销售属性组合
     */
    List<SkuItemSaleAttrVo> saleAttr;

    /**
     * // 4.获取spu的介绍 pms_spu_info_desc
     */
    SpuInfoDescEntity desc;

    /**
     * / 5.获取spu的规格参数信息
     */
    List<SpuItemAttrGroupVo> groupAttrs;

    /**
     * 6 当前商品秒杀的优惠信息
     */
    private SeckillSkuInfoVo seckillSkuInfoVo;


}
