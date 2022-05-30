package com.atguigu.gulimall.order.vo;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * @author Peter
 * @date 2022/5/27 17:00
 * @description Usage
 */
@Data
@ToString
public class FareVo {

    private MemberAddressVo address;
    private BigDecimal fare;
}
