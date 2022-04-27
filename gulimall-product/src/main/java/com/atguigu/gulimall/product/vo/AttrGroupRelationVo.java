package com.atguigu.gulimall.product.vo;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/4/26 17:08
 * @description TODO
 */
@Data
public class AttrGroupRelationVo {
    /**
     * [{"attrId":1,"attrGroupId":2}]
     */
    private Long attrId;
    private Long attrGroupId;

}
