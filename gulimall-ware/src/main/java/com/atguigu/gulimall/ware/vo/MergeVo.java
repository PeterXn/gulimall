package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/5/2 23:29
 * @description 合并采购需求VO
 */
@Data
public class MergeVo {
    //整单id
    private Long purchaseId;
    //合并项集合
    private List<Long> items;
}
