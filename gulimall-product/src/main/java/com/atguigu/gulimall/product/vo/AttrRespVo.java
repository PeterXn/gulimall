package com.atguigu.gulimall.product.vo;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/4/25 23:40
 * @description TODO
 */
@Data
public class AttrRespVo extends AttrVo{

    /**
     * 	"catelogName": "手机/数码/手机", //所属分类名字
     * 	"groupName": "主体", //所属分组名字
     */
    private String catelogName;
    private String groupName;

    private Long[] catelogPath;
}
