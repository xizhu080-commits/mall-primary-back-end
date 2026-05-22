package com.mall.demo.module.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment")
public class Payment {

    @TableId(type = IdType.ASSIGN_ID)
    private String paymentId;


    private String orderId;
    // 订单号🔥（关联订单）

    private String userId;
    // 用户ID

    private BigDecimal amount;
    // 支付金额🔥

    private String payType;
    // 支付方式（ALIPAY / WECHAT）

    private Integer status;
    // 状态 0:未支付 1:已支付

    private String tradeNo;
    // 第三方交易号🔥（支付宝/微信返回）
    // 订单回调信息
    private String callbackContent;

    private LocalDateTime payTime;
    // 支付时间

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}