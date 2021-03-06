package com.atguigu.gulimall.order.interceptor;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.vo.MemberRespVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Peter
 * @date 2022/5/24 9:38
 * @description 拦截用户登录请求
 */
@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    /**
     * 当前线程内共享数据
     */
    public static ThreadLocal<MemberRespVo> loginUser = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //解锁库存时查询订单的远程调用
        //请求路径是/order/order/status/{orderSn}，直接放行
        String uri = request.getRequestURI();
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        boolean match = antPathMatcher.match("/order/order/status/**", uri);
        //放行支付宝异步通知请求
        boolean payedMatch = antPathMatcher.match("/payed/notify", uri);
        if (match || payedMatch) {
            return true;
        }


        MemberRespVo attribute = (MemberRespVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
        if (attribute != null) {
            //已登录
            loginUser.set(attribute);
            return true;
        } else {
            //没有登录，提示登录
            request.getSession().setAttribute("msg", "您未登录，请先登录");
            response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }
    }
}
