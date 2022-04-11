package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author peter
 * @email test@gmail.com
 * @date 2022-04-11 13:06:39
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
