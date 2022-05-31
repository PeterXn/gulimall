package com.atguigu.gulimall.member.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * @author Peter
 * @date 2022/5/31 0:54
 * @description Usage
 */
@FeignClient("gulimall-order")
public interface OrderFeignService {

    @RequestMapping("/order/order/listWithItem")
    R listWithItem(@RequestBody Map<String, Object> params);
}
