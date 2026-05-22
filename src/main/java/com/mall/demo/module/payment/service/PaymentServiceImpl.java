package com.mall.demo.module.payment.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.mall.demo.common.enums.ErrorCodeEnum;
import com.mall.demo.common.exception.BizException;
import com.mall.demo.common.util.SecurityUtils;
import com.mall.demo.module.Refund.entity.Refund;
import com.mall.demo.module.TransactionRecord.entity.TransactionRecord;
import com.mall.demo.module.TransactionRecord.mapper.TransactionRecordMapper;
import com.mall.demo.module.order.dto.req.OrderItemReqDto;
import com.mall.demo.module.order.entity.MyOrder;
import com.mall.demo.module.order.entity.OrderItem;
import com.mall.demo.module.order.entity.Suborder;
import com.mall.demo.module.order.mapper.OrderItemMapper;
import com.mall.demo.module.order.mapper.OrderMapper;
import com.mall.demo.module.order.mapper.SuborderMapper;
import com.mall.demo.module.order.service.StockService;
import com.mall.demo.module.payment.dto.req.PayReqDto;
import com.mall.demo.module.payment.dto.resp.PayRespDto;
import com.mall.demo.module.payment.entity.Payment;
import com.mall.demo.module.payment.mapper.PaymentMapper;
import com.mall.demo.module.product.entity.SKU;
import com.mall.demo.module.product.mapper.SKUMapper;
import com.mall.demo.module.shop.entity.Shop;
import com.mall.demo.module.shop.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;

    private final OrderItemMapper orderItemMapper;
    private final SKUMapper skuMapper;
    private final AlipayClient alipayClient;
    private final TransactionRecordMapper transactionRecordMapper;
    private final SuborderMapper suborderMapper;
    private final ShopMapper shopMapper;
    private final PaymentNotifyMessageService paymentNotifyMessageService;


    // 修复：对应的 YAML 路径要写全
    @Value("${alipay.sandbox.notifyUrl}")
    private String notifyUrl;

    @Value("${alipay.sandbox.alipayPublicKey}")
    private String alipayPublicKey;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayRespDto createPayment(PayReqDto dto) {


        String userId = SecurityUtils.getId();
        if (userId == null) {
            throw new BizException(ErrorCodeEnum.USER_NOT_EXIST.getCode(),
                    ErrorCodeEnum.USER_NOT_EXIST.getMessage());
        }
        String orderId = dto.getOrderId();
        MyOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCodeEnum.ORDER_NOT_EXIST.getCode(),
                    ErrorCodeEnum.ORDER_NOT_EXIST.getMessage());
        }
        if (!order.getUserId().matches(userId)) {
            throw new BizException(ErrorCodeEnum.NOT_PERMISSION.getCode(),
                    ErrorCodeEnum.NOT_PERMISSION.getMessage());
        }

        BigDecimal payAmount = order.getPayAmount();

        // 1. 查询是否存在支付单
        LambdaQueryWrapper<Payment> paymentWrapper = new LambdaQueryWrapper<>();
        paymentWrapper.eq(Payment::getOrderId, dto.getOrderId())
                .eq(Payment::getUserId, userId);
        Payment existPayment = paymentMapper.selectOne(paymentWrapper);

        Payment payment = null;

        // 2. 支付单已存在的情况
        if (existPayment != null) {


            // 已支付：直接抛异常
            if (existPayment.getStatus() == 1) {
                throw new BizException(ErrorCodeEnum.PAYMENT_ALREADY_PAY.getCode(),
                        ErrorCodeEnum.PAYMENT_ALREADY_PAY.getMessage());
            }
            // 未支付：复用现有支付单
            if (existPayment.getStatus() == 0) {
                payment = existPayment;
                log.info("复用已有支付单: {}", payment.getPaymentId());
            } else {
                throw new BizException(ErrorCodeEnum.PAYMENT_CLOSED.getCode(),
                        ErrorCodeEnum.PAYMENT_CLOSED.getMessage());
            }
        } else {
            // 3. 支付单不存在：创建新的
            payment = new Payment();
            payment.setOrderId(dto.getOrderId());
            payment.setUserId(userId);
            payment.setPayType(dto.getPayType());
            payment.setAmount(payAmount);
            payment.setStatus(0);
            payment.setCreateTime(LocalDateTime.now());
            paymentMapper.insert(payment);
            log.info("创建新支付单: {}", payment.getPaymentId());
        }

        // 4. 构建支付宝预下单请求
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
        request.setNotifyUrl(notifyUrl);

        JSONObject bizContent = new JSONObject();
        bizContent.set("out_trade_no", payment.getPaymentId());
        bizContent.set("total_amount", payAmount.setScale(2, RoundingMode.HALF_UP).toString());
        bizContent.set("subject", "商城订单支付-" + dto.getOrderId());
        // 设置二维码超时时间为 2 分钟
        bizContent.set("timeout_express", "2m");
        request.setBizContent(bizContent.toString());

        try {
            AlipayTradePrecreateResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("支付宝预下单成功，支付单号: {}，等待扫码支付...", payment.getPaymentId());

                PayRespDto respDto = new PayRespDto();
                respDto.setOrderId(dto.getOrderId());
                respDto.setPaymentId(payment.getPaymentId());
                respDto.setUserId(userId);
                respDto.setPayType(dto.getPayType());
                respDto.setPayAmount(payAmount);
                respDto.setPayTime(LocalDateTime.now());

                String qrLink = response.getQrCode();
                log.info("QR码:{}",response.getQrCode());

                respDto.setQrCode(generateQrCodeBase64(qrLink));
                log.info("二维码base64:{}",generateQrCodeBase64(qrLink));
                //respDto.setQrCode(response.getQrCode());
                respDto.setPayAmount(payAmount);
                return respDto;
            } else {
                log.error("支付宝应答失败: {}", response.getBody());
                throw new BizException("支付宝接口调用失败: " + response.getBody());
            }
        } catch (AlipayApiException e) {
            log.error("支付宝调用异常", e);
            throw new BizException("支付网关异常，请稍后重试");
        }
    }


    // 状态 0:未支付 1:已支付
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String processAlipayNotify(Map<String, String> params) {
// 【新增】确认回调是否到达
        log.info("==========================================");
        log.info("【支付宝回调】收到通知！交易状态：{}", params.get("trade_status"));
        log.info("==========================================");


        try {
            // 1. 安全验签
            boolean verifyResult = AlipaySignature.rsaCheckV1(
                    params,
                    alipayPublicKey,
                    "UTF-8",
                    "RSA2"
            );

            if (!verifyResult) {
                log.error("支付宝回调验签失败！");
                return "failure";
            }

            // 2. 检查支付状态
            String tradeStatus = params.get("trade_status");
            if (!"TRADE_SUCCESS".equals(tradeStatus)) {
                return "success";
            }

            // 3. 获取单号
            String paymentNo = params.get("out_trade_no");
            Payment payment = paymentMapper.selectOne(
                    new LambdaQueryWrapper<Payment>().eq(Payment::getPaymentId, paymentNo)
            );


            // 状态 0:未支付 1:已支付
            // 4. 幂等处理
            if (payment == null) {
                log.error("支付单 {} 不存在", paymentNo);
                return "failure";
            }
            if (payment.getStatus() == 1) {

                List<Suborder> suborderList = suborderMapper.getSuborderListByIdOrderId(payment.getOrderId());
                for (Suborder suborder : suborderList) {
                    String shopId = suborder.getShopId();
                    Shop shop = shopMapper.selectById(shopId);
                    // 通知商户---支付成功通知---请发货!
                    paymentNotifyMessageService.notifyMerchantForShip(payment.getPaymentId(), suborder.getSuborderId(), shop.getMerchantId());


                }

                return "success";
            }

            //===============================支付成功,更新有关数据库数据=============================================
            //==========MyOrder表,SKU表,Payment表======================================================

            // 状态 0:未支付 1:已支付
            // 5. 更新本地支付单
            payment.setStatus(1);
            payment.setTradeNo(params.get("trade_no"));
            payment.setPayTime(LocalDateTime.now());
            // 提示：JSON.toJSONString 需要引入 Fastjson 或 Jackson，这里用 Hutool 的
            payment.setCallbackContent(new JSONObject(params).toString());
            paymentMapper.updateById(payment);
            log.info("支付 {} 状态已更新为已支付(2)", payment.getOrderId());


            // 6. 更新本地订单状态
            int orderUpdated = orderMapper.update(
                    null,
                    new LambdaUpdateWrapper<MyOrder>()
                            .set(MyOrder::getStatus, 2)
                            .set(MyOrder::getUpdateTime, LocalDateTime.now())
                            .eq(MyOrder::getOrderId, payment.getOrderId())
                            .eq(MyOrder::getStatus, 1)
            );
            StringBuilder productNameText = new StringBuilder();
            if (orderUpdated > 0) {
                log.info("订单 {} 状态已更新为待发货(2)", payment.getOrderId());

                List<OrderItem> orderItems = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, payment.getOrderId())
                );

                // 7. 更新SKU库存
                for (OrderItem item : orderItems) {
                    skuMapper.update(
                            null,
                            new LambdaUpdateWrapper<SKU>()
                                    .setSql("stock = stock - " + item.getQuantity())
                                    .eq(SKU::getSkuId, item.getSkuId())
                                    .ge(SKU::getStock, item.getQuantity())
                    );
                    log.info("商品SKU {} 库存扣减 {}", item.getSkuId(), item.getQuantity());

                    //获取商品名称文本
                    if (productNameText.length() > 0) {
                        productNameText.append(" | ");
                    }
                    productNameText.append(item.getProductName());

                }
            } else {
                log.warn("订单 {} 状态更新失败或已被处理", payment.getOrderId());
            }

            // 8. 创建交易记录
            List<Suborder> suborderList = suborderMapper.getSuborderListByIdOrderId(payment.getOrderId());
            for (Suborder suborder : suborderList) {
                TransactionRecord transactionRecord = new TransactionRecord();
                transactionRecord.setUserId(payment.getUserId());
                transactionRecord.setPayAmount(payment.getAmount());
                transactionRecord.setTransactionStatus(1);
                transactionRecord.setOrderId(payment.getOrderId());
                transactionRecord.setPayTime(payment.getPayTime());
                transactionRecord.setPayType(payment.getPayType());
                transactionRecord.setProductNameText(productNameText.toString());
                transactionRecord.setSuborderId(suborder.getSuborderId());
                transactionRecord.setUpdateTime(LocalDateTime.now());
                transactionRecord.setRemark("支付成功--交易成功");
                transactionRecordMapper.insert(transactionRecord);
                log.info("交易记录 {} 已创建", transactionRecord.getTransactionId());
            }



            suborderMapper.update(
                    null,
                    new LambdaUpdateWrapper<Suborder>()
                            .set(Suborder::getStatus, 2)
                            .set(Suborder::getUpdateTime, LocalDateTime.now())
                            .eq(Suborder::getOrderId, payment.getOrderId())
            );


            // 9. 【新增】通过 WebSocket 实时通知买家（用户）支付成功
            // 注意：这里需要注入 SimpMessagingTemplate 或者调用 paymentNotifyMessageService
            // 建议在 PaymentNotifyMessageService 接口中增加一个 notifyUser 方法
            log.info("【调试】准备进入用户通知模块...");
            try {

                log.info("【准备推送】正在向用户 {} 发送支付成功通知", payment.getUserId());
                List<String> suborderIds = suborderList.stream()
                        .map(Suborder::getSuborderId)
                        .collect(java.util.stream.Collectors.toList());
                String suborderIdJson = JSON.toJSONString(suborderIds);

                String title = "新的支付通知--支付成功!";
                String content = buildLogisticNotifyJson(suborderList, payment, title);

                paymentNotifyMessageService.notifyUserForPayment(paymentNo, payment.getUserId(), payment.getOrderId(), suborderIdJson);
                log.info("【推送成功】用户 {} 支付成功通知已发送", payment.getUserId());
            } catch (Exception e) {
                log.error("【推送失败】WebSocket 通知用户失败，异常信息：{}", e.getMessage(), e);
            }



            // 6. 更新业务订单状态 (此处可以调用订单服务的接口)
            log.info("支付单 {} 客户已完成支付,处理成功！", paymentNo);

            return "success";

        } catch (AlipayApiException e) {
            log.error("支付宝回调验签异常", e);
            return "failure";
        }
    }


    /**
     * 将二维码链接转换为 Base64 图片格式
     *
     * @param content 二维码内容（支付宝返回的链接）
     * @return data:image/png;base64,... 格式的字符串
     */
    private String generateQrCodeBase64(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        // 配置二维码尺寸和边距
        QrConfig config = new QrConfig(300, 300);
        config.setMargin(2);

        // 生成 Base64 字符串
        String base64Image = QrCodeUtil.generateAsBase64(content, config, "png");

        // 拼接 Data URI 前缀
        return "data:image/png;base64," + base64Image;
    }



















    /**
     * ✅ 抽取公共方法：构建物流通知 JSON 内容
     *
     * @param suborderList 子订单信息列表
     * @param payment 支付单信息
     * @param title 通知标题
     * @return JSON 格式的通知内容
     */
    private String buildLogisticNotifyJson(List<Suborder> suborderList, Payment payment, String title) {
        JSONObject contentJson = JSONUtil.createObj();

        // 消息类型和标题
        contentJson.set("type", "PAYMENT_NOTIFY");
        contentJson.set("title", title);
        contentJson.set("orderId", payment.getOrderId());

        // ✅ 商品信息列表（支持多个子订单）
        List<JSONObject> productList = new ArrayList<>();
        for (Suborder suborder : suborderList) {
            JSONObject productJson = JSONUtil.createObj();
            productJson.set("orderId", suborder.getOrderId());
            productJson.set("suborderId", suborder.getSuborderId());
            productJson.set("shopId", suborder.getShopId());
            productJson.set("shopName", suborder.getShopName());
            productJson.set("skuId", suborder.getSkuId());
            productJson.set("productName", suborder.getProductName());
            productJson.set("spec", suborder.getSpecData() != null ? suborder.getSpecData() : "无");
            productJson.set("price", suborder.getPrice());
            productJson.set("payAmount", suborder.getPayAmount());
            productJson.set("productUrl", suborder.getProductUrl());
            productList.add(productJson);
        }
        contentJson.set("products", productList);

        // 支付消息
        JSONObject paymentJson = JSONUtil.createObj();
        paymentJson.set("paymentId", payment.getPaymentId());
        paymentJson.set("orderId", payment.getOrderId());
        paymentJson.set("userId", payment.getUserId());
        paymentJson.set("amount", payment.getAmount());
        paymentJson.set("payType", payment.getPayType());
        paymentJson.set("status", payment.getStatus());
        paymentJson.set("tradeNo", payment.getTradeNo());
        paymentJson.set("payTime", payment.getPayTime());
        contentJson.set("payment", paymentJson);


        // ✅ 收货人信息（取第一个子订单的收货信息，因为同一订单收货人相同）
        if (!suborderList.isEmpty()) {
            Suborder firstSuborder = suborderList.get(0);
            JSONObject receiverJson = JSONUtil.createObj();
            receiverJson.set("receiverName", firstSuborder.getReceiverName());
            receiverJson.set("address", firstSuborder.getAddress());
            receiverJson.set("receiverPhone", firstSuborder.getReceiverPhone());
            contentJson.set("receiver", receiverJson);
        }

        return contentJson.toString();
    }







}