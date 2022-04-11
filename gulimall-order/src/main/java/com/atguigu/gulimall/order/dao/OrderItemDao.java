package com.atguigu.gulimall.order.dao;

import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单项信息
 * 
 * @author peter
 * @email test@gmail.com
 * @date 2022-04-11 17:25:10
 */
@Mapper
public interface OrderItemDao extends BaseMapper<OrderItemEntity> {
	
}
