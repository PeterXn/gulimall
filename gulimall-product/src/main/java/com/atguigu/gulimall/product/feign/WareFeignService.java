package com.atguigu.gulimall.product.feign;

import com.atguigu.common.to.SkuHasStockVo;
import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/5/7 0:59
 * @description description
 */
@FeignClient("gulimall-ware")
public interface WareFeignService {

    @PostMapping("/ware/waresku/hasStock")
    public R getSkusHasStock(@RequestBody List<Long> skuIds);
}
