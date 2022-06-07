package com.atguigu.gulimall.seckill.controller;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.seckill.service.SeckillService;
import com.atguigu.gulimall.seckill.to.SeckillSkuRedisTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Peter
 * @date 2022/6/2 14:17
 * @description 页面显示秒杀的数据来源
 */
@Controller
public class SeckillController {

    @Autowired
    SeckillService seckillService;

    /**
     * 返回当前时间内可以参与秒杀的商品
     */
    @ResponseBody
    @GetMapping("/currentSeckillSkus")
    public R getCurrentSeckillSkus() {

        List<SeckillSkuRedisTo> vos = seckillService.getCurrentSeckillSkus();

        return R.ok().setData(vos);
    }


    /**
     * 查询这个商品是否参与秒杀，在商品详情页显示。
     * 供商品服务远程调用
     * @param skuId
     * @return
     */
    @ResponseBody
    @GetMapping("/sku/seckill/{skuId}")
    public R getSkuSeckillInfo(@PathVariable("skuId") Long skuId) {

        SeckillSkuRedisTo to = seckillService.getSkuSeckillInfo(skuId);

        return R.ok().setData(to);
    }

    /**
     * 处理秒杀请求
     */
    @GetMapping("/kill")
    public String secKill(@RequestParam("killId") String killId,
                          @RequestParam("key") String key,
                          @RequestParam("num") Integer num,
                          Model model) {
        //http://seckill.gulimall.com/kill?killId=2_1&key=da120fd6c6dd4077985775d9ff79b0dd&num=1

        String orderSn = seckillService.kill(killId, key, num);

        model.addAttribute("orderSn", orderSn);

        return "success";
    }
}
