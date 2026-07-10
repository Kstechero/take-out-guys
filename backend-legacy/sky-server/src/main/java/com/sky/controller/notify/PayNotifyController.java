package com.sky.controller.notify;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.properties.WeChatProperties;
import com.sky.service.OrderService;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付通知接口。
 */
@RestController
@RequestMapping("/notify")
@Slf4j
public class PayNotifyController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private WeChatProperties weChatProperties;

    /**
     * 接收微信支付成功通知，解密订单号并更新订单状态。
     */
    @PostMapping(value = "/paySuccess", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> paySuccessNotify(HttpServletRequest request) throws Exception {
        String body = readBody(request);
        log.info("收到微信支付回调：{}", body);

        JSONObject resource = JSON.parseObject(body).getJSONObject("resource");
        AesUtil aesUtil = new AesUtil(
                weChatProperties.getApiV3Key().getBytes(StandardCharsets.UTF_8));
        String plainText = aesUtil.decryptToString(
                resource.getString("associated_data").getBytes(StandardCharsets.UTF_8),
                resource.getString("nonce").getBytes(StandardCharsets.UTF_8),
                resource.getString("ciphertext"));

        JSONObject payment = JSON.parseObject(plainText);
        String orderNumber = payment.getString("out_trade_no");
        log.info("微信支付成功，订单号：{}，微信交易号：{}",
                orderNumber, payment.getString("transaction_id"));
        orderService.paySuccess(orderNumber);

        Map<String, String> response = new HashMap<>();
        response.put("code", "SUCCESS");
        response.put("message", "SUCCESS");
        return response;
    }

    /** 读取微信回调请求体。 */
    private String readBody(HttpServletRequest request) throws Exception {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }
}
