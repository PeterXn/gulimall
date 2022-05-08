package com.atguigu.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/5/8 23:21
 * @description 2级分类Vo
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Catelog2Vo {

    /**
     * 一级父分类的id
     */
    private String catalog1Id;

    /**
     * 三级子分类
     */
    private List<Catelog3Vo> catalog3List;

    private String id;

    private String name;


    /**
     * 三级分类vo
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Catelog3Vo {

        /**
         * 父分类、二级分类id
         */
        private String catalog2Id;

        private String id;

        private String name;
    }
}
