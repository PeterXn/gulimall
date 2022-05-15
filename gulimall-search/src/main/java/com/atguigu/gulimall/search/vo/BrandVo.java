package com.atguigu.gulimall.search.vo;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/5/15 19:11
 * @description 接受product系统的远程数据
 */
@Data
public class BrandVo {

    /**
     * "brandId": 0,
     * "brandName": "string",
     */
    private Long brandId;
    /**
     * BrandEntity的属性为name
     */
    private String name;
}
