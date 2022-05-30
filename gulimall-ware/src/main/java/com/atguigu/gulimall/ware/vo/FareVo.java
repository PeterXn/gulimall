package com.atguigu.gulimall.ware.vo;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * @author Peter
 * @date 2022/5/25 16:37
 * @description 返回给页面，地址加运费
 */
@Data
@ToString
public class FareVo {
    private MemberAddressVo address;
    private BigDecimal fare;
}
