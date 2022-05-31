package com.atguigu.gulimall.order.listener;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gulimall.order.config.AlipayTemplate;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.PayAsyncVo;
import com.atguigu.gulimall.order.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Peter
 * @date 2022/5/31 16:16
 * @description 处理支付宝异步通知，必须有公网
 */
@RestController
public class OrderPayedListener {

    @Autowired
    OrderService orderService;

    @Autowired
    AlipayTemplate alipayTemplate;

    /**
     * 处理支付宝异步返回请求
     * //TODO 异步通知需要公网
     */
    @PostMapping("/payed/notify")
    public String handleAlipay(PayAsyncVo vo,HttpServletRequest request) throws AlipayApiException {

        Map<String, String[]> map = request.getParameterMap();
        for (String key : map.keySet()) {
            String value = request.getParameter(key);
            System.out.println("参数名：" + key + "==>参数值：" + value);
        }
        System.out.println("支付宝通知已到位...数据：" + map);

        //TODO 需要验签
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            //乱码解决，这段代码在出现乱码时使用
            // valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
            params.put(name, valueStr);
        }

        //调用SDK验证签名
        boolean signVerified = AlipaySignature.rsaCheckV1(params, alipayTemplate.getAlipay_public_key(),
                alipayTemplate.getCharset(), alipayTemplate.getSign_type());

        if (signVerified) {
            System.out.println("支付宝签名验证成功");
            String result = orderService.handlePayResult(vo);
            return result;
        } else {
            System.out.println("支付宝签名验证失败");
            return "error";
        }
    }

}
