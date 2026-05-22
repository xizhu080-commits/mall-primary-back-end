package com.mall.demo.module.payment.controller;

import com.mall.demo.common.result.RestResp;
import com.mall.demo.module.payment.dto.req.PayReqDto;
import com.mall.demo.module.payment.dto.resp.PayRespDto;
import com.mall.demo.module.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/pay/alipay")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 1. 发起支付接口
     * 前端 Vue 3 调用此接口，获取支付二维码链接
     */
    @PostMapping("/create")
    public RestResp<PayRespDto> createPayment(@RequestBody PayReqDto dto) {
        log.info("收到支付请求，订单号：{}", dto.getOrderId());
        return RestResp.ok(paymentService.createPayment(dto));
    }

    /**
     * 2. 支付宝回调接口
     * 注意：这个接口是给支付宝服务器调用的，必须是 POST 形式
     * 地址必须与你 application.yml 中的 notifyUrl 完全一致
     */
    @PostMapping("/notify")
    public RestResp<String> handleAlipayNotify(HttpServletRequest request) {
        // 将支付宝传来的 HttpRequest 参数转为 Map
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();

        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }

        log.info("接收到支付宝异步通知，交易状态：{}", params.get("trade_status"));

        // 调用 Service 处理验签和业务逻辑
        // 如果处理成功返回 "success"，处理失败返回 "failure"
        return RestResp.ok(paymentService.processAlipayNotify(params));
    }
}