package com.atguigu.gulimall.search.service;

import com.atguigu.common.to.es.SkuEsModel;

import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/5/7 1:46
 * @description description
 */
public interface ProductSaveService {
    boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException;
}
