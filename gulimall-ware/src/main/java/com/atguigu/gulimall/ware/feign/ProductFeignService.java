package com.atguigu.gulimall.ware.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/5/3 23:01
 * @description 远程调用Product系统
 */
@FeignClient("gulimall-product")
public interface ProductFeignService {

    /**
     *
     * 1) 让所有请求过网关
     *    1、@FeignClient("gulimall-gateway")，给gulimall-gateway所在机器发请求
     *    2、/api/product/skuinfo/info/{skuId}
     *
     * 2） 直接让后台指定处理
     *    1、@FeignClient("gulimall-product")
     *    2、/product/skuinfo/info/{skuId}
     *
     * @param skuId
     * @return
     */
    @RequestMapping("/product/skuinfo/info/{skuId}")
    public R info(@PathVariable("skuId") Long skuId);

}
