package com.atguigu.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.cart.feign.ProductFeignService;
import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.Cart;
import com.atguigu.gulimall.cart.vo.CartItem;
import com.atguigu.gulimall.cart.vo.SkuInfoVo;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @author Peter
 * @date 2022/5/22 1:20
 * @description Usage
 */
@Service
@Slf4j
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    private final String CART_PREFIX = "gulimall:cart:";

    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String res = (String) cartOps.get(skuId.toString());
        if (StringUtils.isEmpty(res)) {
            //购物车无此商品，则添加

            //添加新商品到购物车
            CartItem cartItem = new CartItem();

            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                //1.远程查询当前要添加的商品的信息
                R skuInfo = productFeignService.getSkuInfo(skuId);
                SkuInfoVo data = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });
                cartItem.setImage(data.getSkuDefaultImg());
                cartItem.setTitle(data.getSkuTitle());
                cartItem.setPrice(data.getPrice());

                cartItem.setCheck(true);
                cartItem.setCount(num);
                cartItem.setSkuId(skuId);
            }, executor);


            //2.远程查询sku的组合属性信息
            CompletableFuture<Void> getSkuSaleAttrValues = CompletableFuture.runAsync(() -> {
                List<String> values = productFeignService.getSkuSaleAttrValues(skuId);
                cartItem.setSkuAttrs(values);
            }, executor);

            //等待2个异步任务完成
            CompletableFuture.allOf(getSkuInfoTask, getSkuSaleAttrValues).get();

            //更新redis
            String jsonString = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(), jsonString);

            return cartItem;
        } else {
            //购物车存在当前商品，则更新数量
            CartItem cartItem = JSON.parseObject(res, CartItem.class);
            cartItem.setCount(cartItem.getCount() + num);
            //更新redis
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));

            return cartItem;
        }
    }

    @Override
    public CartItem getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String str = (String) cartOps.get(skuId.toString());
        CartItem cartItem = JSON.parseObject(str, CartItem.class);

        return cartItem;
    }

    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        Cart cart = new Cart();
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();

        if (userInfoTo.getUserId() != null) {
            //1.已登录
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();

            //2.临时购物车还有数据，则合并
            List<CartItem> tempCartItems = getCartItems(tempCartKey);
            if (!CollectionUtils.isEmpty(tempCartItems)) {
                //临时购物车有数据，需要合并
                for (CartItem item : tempCartItems) {
                    addToCart(item.getSkuId(), item.getCount());
                }
                //清除临时购物车的数据
                clearCart(tempCartKey);
            }

            //3.获取登录后的所有购物车数据（包含合并后的数据）
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        } else {
            //2.未登录
            String cartKey = CART_PREFIX + userInfoTo.getUserKey();
            //获取临时购物车的所有购物项
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        }

        return cart;
    }

    @Override
    public void clearCart(String cartKey) {
        redisTemplate.delete(cartKey);
    }

    @Override
    public void checkItem(Long skuId, Integer check) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItem cartItem = getCartItem(skuId);

        //cartItem.setCheck(check == 1 ? true : false);
        cartItem.setCheck(check == 1);

        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(), s);
    }

    @Override
    public void updateItemCount(Long skuId, Integer num) {
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCount(num);

        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
    }

    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        cartOps.delete(skuId.toString());
    }

    @Override
    public List<CartItem> getUserCartItems() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() == null) {
            return null;
        } else {
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            List<CartItem> cartItems = getCartItems(cartKey);
            //获取所有被选中的购物项
            List<CartItem> collect = cartItems.stream()
                    .filter(item -> item.getCheck())
                    .map(item -> {
                        /*
                        从商品服务远程调用查询数据库最新价格
                         */
                        BigDecimal price = productFeignService.getPrice(item.getSkuId());
                        item.setPrice(price);
                        return item;
                    })
                    .collect(Collectors.toList());

            return collect;
        }
    }

    /**
     * 获取要操作的购物车
     *
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            //登录使用id
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        } else {
            //没登录使用userkey
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }

        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);

        return operations;
    }

    private List<CartItem> getCartItems(String cartKey) {
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
        List<Object> values = hashOps.values();
        if (!CollectionUtils.isEmpty(values)) {
            List<CartItem> cartItems = values.stream().map((obj) -> {
                String str = (String) obj;
                CartItem cartItem = JSON.parseObject(str, CartItem.class);

                return cartItem;
            }).collect(Collectors.toList());

            return cartItems;
        }

        return null;
    }
}
