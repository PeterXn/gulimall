package com.atguigu.gulimall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/5/12 16:41
 * @description 封装页面所有可能传递的检索条件
 * <p>
 * keyword=小米&sort=saleCount_desc/asc&hasStock=0/1&skuPrice=400_1900&brandId=1
 * &catalogId=1&attrs=1_3G:4G:5G&attrs=2_骁龙 845&attrs=4_高清屏
 */
@Data
public class SearchParam {

    /**
     * 页面传递过来的全文匹配关键字
     */
    private String keyword;
    /**
     * 本级分类id
     */
    private Long catalog3Id;

    /**
     * sort=saleCount_asc/desc
     * sort=skuPrice_asc/desc
     * sort=hotScore_asc/desc
     * 排序条件 v
     */
    private String sort;

    /**
     *过滤条件
     * hasStock(是否有货)、 skuPrice 区间、 brandId、 catalog3Id、 attrs
     * hasStock=0/1
     * skuPrice=1_500/_500/500_
     * brandId=1
     * attrs=2_5 存:6 寸
     **/

    /**
     * 是否只显示有货 v 0（无库存） 1（有库存）
     */
    private Integer hasStock;
    /**
     * //价格区间查询 v
     */
    private String skuPrice;
    /**
     * //按照品牌进行查询， 可以多选 v
     */
    private List<Long> brandId;
    /**
     * //按照属性进行筛选 v
     */
    private List<String> attrs;

    /**
     * //页码
     */
    private Integer pageNum = 1;
    /**
     * //原生的所有查询条件
     */
    private String _queryString;
}
