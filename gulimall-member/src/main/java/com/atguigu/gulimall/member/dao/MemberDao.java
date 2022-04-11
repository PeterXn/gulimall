package com.atguigu.gulimall.member.dao;

import com.atguigu.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author peter
 * @email test@gmail.com
 * @date 2022-04-11 16:46:51
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
