package com.atguigu.common.constant;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/5/2 23:39
 * @description ware系统常量
 */
public class WareConstant {
    public enum PurchaseStatusEnum {
        CREATED(0,"新建"),
        ASSIGNED(1,"已分配"),
        RECEIVED(2,"已领取"),
        FINISHED(3,"已完成"),
        HASERROR(4,"有异常");

        private Integer code;
        private String msg;

        PurchaseStatusEnum(Integer code, String msg) {
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

    public enum PurchaseDetailStatusEnum {
        CREATED(0,"新建"),
        ASSIGNED(1,"已分配"),
        BUYING(2,"正在采购"),
        FINISHED(3,"已完成"),
        HASERROR(4,"采购失败");

        private Integer code;
        private String msg;

        PurchaseDetailStatusEnum(Integer code, String msg) {
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
