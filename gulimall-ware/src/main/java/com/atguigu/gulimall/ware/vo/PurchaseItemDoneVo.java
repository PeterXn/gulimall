package com.atguigu.gulimall.ware.vo;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/5/3 14:14
 * @description description
 */
@Data
public class PurchaseItemDoneVo {
    /**
     * [{itemId:1,status:4,reason:""}]
     */
    private Long itemId;
    private Integer status;
    private String reason;
}
