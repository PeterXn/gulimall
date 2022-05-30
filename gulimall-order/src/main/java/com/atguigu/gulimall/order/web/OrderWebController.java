package com.atguigu.gulimall.order.web;

import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import com.atguigu.gulimall.order.vo.SubmitOrderResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

/**
 * @author Peter
 * @date 2022/5/24 9:33
 * @description Usage
 */
@Controller
@Slf4j
public class OrderWebController {

    @Autowired
    OrderService orderService;


    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {

        OrderConfirmVo confirmVo = orderService.confirmOrder();

        model.addAttribute("orderConfirmData", confirmVo);

        log.info("confirmVo: [{}]", confirmVo.toString());

        //展示订单确认的数据
        return "confirm";
    }


    /**
     * 提交订单
     *
     * @return
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes ra) {

        SubmitOrderResponseVo responseVo = orderService.submitOrder(vo);

        System.out.println("订单提交的数据 vo = " + vo);

        if (responseVo.getCode() == 0) {
            //下单成功来到支付页面

            model.addAttribute("submitOrderResp", responseVo);
            return "pay";

        } else {
            String msg = "下单失败：";
            //下单失败回到订单确认页面重新确认下单
            switch (responseVo.getCode()) {
                case 1:
                    msg += "令牌验证失败";
                    break;
                case 2:
                    msg += "订单商品价格发生变化，请确认后再提交";
                    break;
                case 3:
                    msg += "商品库存不足，库存锁定商品失败";
                    break;
                default:
                    break;
            }
            ra.addFlashAttribute("msg", msg);
            return "redirect:http://order.gulimall.com/toTrade";
        }

    }
}
