package com.atguigu.gulimall.member.controller;

import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.common.to.SocialUserTo;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.exception.PhoneExistException;
import com.atguigu.gulimall.member.exception.UsernameExistException;
import com.atguigu.gulimall.member.feign.CouponFeignService;
import com.atguigu.gulimall.member.service.MemberService;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberRegistVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;


/**
 * 会员
 *
 * @author peter
 * @email test@gmail.com
 * @date 2022-04-11 16:46:51
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    @Autowired
    private CouponFeignService couponFeignService;

    @RequestMapping("/coupons")
    public R test() {
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setNickname("天下大公");
        R membercoupons = couponFeignService.membercoupons();
        return R.ok().put("member", memberEntity).put("coupons", membercoupons.get("coupons"));
    }

    /**
     * 社交用户使用
     * @param socialUserTo
     * @return
     */
    @PostMapping("/oauth2/login")
    public R oauth2Login(@RequestBody SocialUserTo socialUserTo) throws Exception {

        MemberEntity entity = memberService.login(socialUserTo);

        if (entity != null) {
            //TODO 登录成功后的处理
            return R.ok().setData(entity);
        } else {
            //登录失败
            return R.error(BizCodeEnum.LOGINACCT_PASSWORD_INVALID_EXCEPTION.getCode(),
                    BizCodeEnum.LOGINACCT_PASSWORD_INVALID_EXCEPTION.getMsg());
        }
    }

    /**
     * 远程调用使用
     *
     * @param vo
     * @return
     */
    @PostMapping("/login")
    public R login(@RequestBody MemberLoginVo vo) {

        MemberEntity entity = memberService.login(vo);

        if (entity != null) {
            //TODO 登录成功后的处理
            return R.ok().setData(entity);
        } else {
            //登录失败
            return R.error(BizCodeEnum.LOGINACCT_PASSWORD_INVALID_EXCEPTION.getCode(),
                    BizCodeEnum.LOGINACCT_PASSWORD_INVALID_EXCEPTION.getMsg());
        }
    }

    /**
     * 供远程调用注册使用
     *
     * @return
     */
    @PostMapping("/regist")
    public R regist(@RequestBody MemberRegistVo vo) {

        try {
            memberService.regist(vo);
        } catch (PhoneExistException e) {
            return R.error(BizCodeEnum.PHONE_EXIST_EXCEPTION.getCode(), BizCodeEnum.PHONE_EXIST_EXCEPTION.getMsg());
        } catch (UsernameExistException e) {
            return R.error(BizCodeEnum.USER_EXIST_EXCEPTION.getCode(), BizCodeEnum.USER_EXIST_EXCEPTION.getMsg());
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(BizCodeEnum.UNKNOWN_EXCEPTION.getCode(), BizCodeEnum.UNKNOWN_EXCEPTION.getMsg());
        }

        return R.ok();
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("member:member:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("member:member:info")
    public R info(@PathVariable("id") Long id) {
        MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("member:member:save")
    public R save(@RequestBody MemberEntity member) {
        memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("member:member:update")
    public R update(@RequestBody MemberEntity member) {
        memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("member:member:delete")
    public R delete(@RequestBody Long[] ids) {
        memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
