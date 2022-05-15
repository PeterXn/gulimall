package com.atguigu.gulimall.search.service;

import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/5/12 16:45
 * @description Usage
 */
public interface MallSearchService {
    /**
     *
     * @param param 检索的所有参数
     * @return 返回检索的结果，包含页面需要的所有信息
     */
    SearchResult search(SearchParam param);
}
