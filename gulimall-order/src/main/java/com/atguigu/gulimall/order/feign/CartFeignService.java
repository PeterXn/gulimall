package com.atguigu.gulimall.order.feign;

import com.atguigu.gulimall.order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * @author Peter
 * @date 2022/5/24 17:06
 * @description Usage
 */
@FeignClient("gulimall-cart")
public interface CartFeignService {

    @GetMapping("/currentUserCartItems")
    List<OrderItemVo> getCurrentUserCartItems();
}
