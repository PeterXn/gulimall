package com.atguigu.common.constant;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/4/26 16:10
 * @description product系统常量
 */
public class ProductConstant {
    private static final String BASE_QUERY = "base";
    private static final String SALE_QUERY = "sale";

    public enum AttrEnum {
        ATTR_TYPE_BASE(1,"基本属性"),
        ATTR_TYPE_SALE(0,"销售属性");

        private Integer code;
        private String msg;

        AttrEnum(Integer code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public Integer getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }

    public enum StatusEnum {
        NEW_SPU(0,"新建"),
        SPU_UP(1,"商品上架"),
        SPU_DOWN(2,"商品下架");

        private Integer code;
        private String msg;

        StatusEnum(Integer code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public Integer getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }
}
