package com.atguigu.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.common.to.SocialUserTo;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.member.dao.MemberDao;
import com.atguigu.gulimall.member.dao.MemberLevelDao;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.atguigu.gulimall.member.exception.PhoneExistException;
import com.atguigu.gulimall.member.exception.UsernameExistException;
import com.atguigu.gulimall.member.service.MemberService;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberRegistVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegistVo vo) {
        MemberDao memberDao = this.baseMapper;
        MemberEntity entity = new MemberEntity();

        // 设置默认等级
        MemberLevelEntity levelEntity = memberLevelDao.getDefaultLevel();
        entity.setLevelId(levelEntity.getId());

        // 设置手机号码、姓名;保存之前先查询数据库的唯一性
        checkPhoneUnique(vo.getPhone());
        checkUsernameUnique(vo.getUserName());

        entity.setMobile(vo.getPhone());
        entity.setUsername(vo.getUserName());
        entity.setNickname(vo.getUserName());

        // 密码加密
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassword());
        entity.setPassword(encode);

        entity.setCreateTime(new Date());


        memberDao.insert(entity);
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException {
        MemberDao memberDao = this.baseMapper;
        Integer count = memberDao.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (count > 0) {
            throw new PhoneExistException();
        }
    }

    @Override
    public void checkUsernameUnique(String username) throws UsernameExistException {
        MemberDao memberDao = this.baseMapper;
        Integer count = memberDao.selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
        if (count > 0) {
            throw new UsernameExistException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginacct = vo.getLoginacct();
        String password = vo.getPassword();

        //1.去数据库查询
        MemberDao memberDao = this.baseMapper;
        MemberEntity entity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginacct)
                .or().eq("mobile", loginacct));
        if (entity == null) {
            //登录失败
            return null;
        } else {
            //对比密码
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            String passwordDb = entity.getPassword();
            boolean matches = passwordEncoder.matches(password, passwordDb);
            if (!matches) {
                //密码错误
                return null;
            } else {
                //登录成功
                return entity;
            }
        }
    }

    @Override
    public MemberEntity login(SocialUserTo socialUserTo) throws Exception {

        //登录与注册功能
        String uid = socialUserTo.getUid();
        //1.判读当前社交用户的uid是否在数据库中存在，存在则登录过。
        MemberDao memberDao = this.baseMapper;
        MemberEntity memberEntity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if (memberEntity != null) {
            //注册过则更新
            MemberEntity update = new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(socialUserTo.getAccessToken());
            update.setExpiresIn(socialUserTo.getExpiresIn());

            memberDao.updateById(update);

            memberEntity.setAccessToken(socialUserTo.getAccessToken());
            memberEntity.setExpiresIn(socialUserTo.getExpiresIn());
            return memberEntity;
        } else {
            //准备注册
            MemberEntity regist = new MemberEntity();
            //查询社交用户的账号信息
            try {
                Map<String, String> query = new HashMap<>();
                query.put("access_token", socialUserTo.getAccessToken());
                query.put("uid", socialUserTo.getUid());
                HttpResponse response = HttpUtils.doGet("https://api.weibo.com", "/2/users/show.json",
                        "get", new HashMap<String, String>(), query);
                if (response.getStatusLine().getStatusCode() == 200) {
                    //查询成功
                    String json = EntityUtils.toString(response.getEntity());
                    JSONObject jsonObject = JSON.parseObject(json);
                    System.out.println("jsonObject = " + jsonObject);
                    String name = jsonObject.getString("name");
                    String gender = jsonObject.getString("gender");
                    //..........
                    regist.setNickname(name);
                    regist.setGender("m".equals(gender) ? 1 : 0);
                    //...........

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            regist.setCreateTime(new Date());
            regist.setSocialUid(socialUserTo.getUid());
            regist.setAccessToken(socialUserTo.getAccessToken());
            regist.setExpiresIn(socialUserTo.getExpiresIn());

            memberDao.insert(regist);

            return regist;
        }

    }
}