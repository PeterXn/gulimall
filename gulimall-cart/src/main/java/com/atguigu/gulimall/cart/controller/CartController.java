package com.atguigu.gulimall.cart.controller;

import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.Cart;
import com.atguigu.gulimall.cart.vo.CartItem;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import org.bouncycastle.math.raw.Mod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.concurrent.ExecutionException;

/**
 * @author Peter
 * @date 2022/5/22 1:27
 * @description Usage
 */
@Controller
public class CartController {

    @Autowired
    CartService cartService;

    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId) {

        cartService.deleteItem(skuId);

        return "redirect:http://cart.gulimall.com/cart.html";
    }

    /**
     * 修改购物项数量
     * @return
     */
    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num) {

        cartService.updateItemCount(skuId, num);

        return "redirect:http://cart.gulimall.com/cart.html";
    }

    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("check") Integer check) {

        cartService.checkItem(skuId, check);

        return "redirect:http://cart.gulimall.com/cart.html";
    }


    /**
     * 已登录：session中含有登录信息；
     * 没有登录：从cookie获取user-key来处理；
     * 第一次：如果没有临时用户，帮忙创建一个临时用户。
     *
     * @param model
     * @return 返回到cartList.html
     */
    @GetMapping("/cart.html")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {
        //快速得到用户信息，id,user-key
        //UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();

        Cart cart = cartService.getCart();
        model.addAttribute("cart", cart);

        return "cartList";
    }

    /**
     * 添加商品到购物车
     * RedirectAttributes.addFlashAttribute(): 添加session数据，数据只能取一次
     * RedirectAttributes.addAttribute("skuId", skuId):
     * 将数据放在路径变量上(http://cart.gulimall.com/addToCartSuccessPage.html?skuId=1)
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num,
                            RedirectAttributes ra ) throws ExecutionException, InterruptedException {

        cartService.addToCart(skuId, num);
        //Required Long parameter 'skuId' is not present]
        //model.addAttribute("skuId", skuId);

        ra.addAttribute("skuId", skuId);

        return "redirect:http://cart.gulimall.com/addToCartSuccessPage.html";
    }

    /**
     * 跳转到成功页
     *
     * @param skuId
     * @param model
     * @return
     */
    @GetMapping("/addToCartSuccessPage.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId, Model model) {

        //重新定向到成功页面，再次查询购物车数据即可
        CartItem cartItem = cartService.getCartItem(skuId);
        model.addAttribute("item", cartItem);

        return "success";
    }


}
