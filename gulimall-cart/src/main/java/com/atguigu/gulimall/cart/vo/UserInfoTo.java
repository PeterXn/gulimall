package com.atguigu.gulimall.cart.vo;

import lombok.Data;
import lombok.ToString;

/**
 * @author Peter
 * @date 2022/5/22 2:04
 * @description Usage
 */
@Data
@ToString
public class UserInfoTo {

    private Long userId;
    /**
     * 无论登录与否都会有的字段
     */
    private String userKey;

    private boolean tempUser = false;
}
