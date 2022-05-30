package com.atguigu.gulimall.order.vo;

import com.atguigu.gulimall.order.entity.OrderEntity;
import lombok.Data;
import lombok.ToString;

/**
 * @author Peter
 * @date 2022/5/27 16:00
 * @description Usage
 */
@Data
@ToString
public class SubmitOrderResponseVo {

    private OrderEntity order;
    /**
     * 错误状态码(0:成功，不是0是失败)
     */
    private Integer code = 0;
}
