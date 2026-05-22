package com.mall.demo.module.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
@TableName("payment_notify_message")
public class PaymentNotifyMessage {


    @TableId(type = IdType.ASSIGN_ID)
    private long id;


    private String notifyId;

    // 主订单ID
    private String orderId;

    // 子订单id
    private String suborderId;

    // 支付单ID
    private String paymentId;

    // 目标用户（商家ID 或 用户ID）
    private String targetUserId;


    private String targetUserType;
    // "MERCHANT" 或 "USER"

    // 通知内容
    private String title;
    // 通知标题
    private String content;
    // 通知内容


    // 推送状态'推送状态: 0-待推送,1-推送成功,2-推送失败'
    private Integer pushStatus;

    // 重试次数
    private Integer retryCount;



    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean isRead;
    private LocalDateTime readTime;
    // 是否已读（站内信用）
}
