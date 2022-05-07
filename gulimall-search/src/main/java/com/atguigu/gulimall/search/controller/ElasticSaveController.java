package com.atguigu.gulimall.search.controller;

import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/5/7 1:40
 * @description description
 */
@Slf4j
@RequestMapping("/search/save")
@RestController
public class ElasticSaveController {

    @Autowired
    ProductSaveService productSaveService;

    /**
     * 上架商品
     *
     * @param skuEsModels
     * @return
     */
    @PostMapping("/product")
    public R productStatusUp(@RequestBody List<SkuEsModel> skuEsModels) {
        boolean b = false;
        try {
            b = productSaveService.productStatusUp(skuEsModels);
        } catch (IOException e) {
            log.error("ElasticSaveController商品上架异常：{}", e);
            return R.error(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnum.PRODUCT_UP_EXCEPTION.getMsg());
        }

        if (b) {
            return R.error(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnum.PRODUCT_UP_EXCEPTION.getMsg());
        } else {
            return R.ok();
        }

    }
}
