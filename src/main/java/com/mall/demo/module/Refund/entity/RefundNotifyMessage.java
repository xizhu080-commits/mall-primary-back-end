package com.mall.demo.module.Refund.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("refund_notify_message")
public class RefundNotifyMessage {

    @TableId(type = IdType.AUTO)
    private long id;



    private String notifyId;

    private String refundId;
    // 退款单ID
    private String targetUserId;
    // 目标用户（商家ID 或 用户ID）
    private String targetUserType;
    // "MERCHANT" 或 "USER"

    // 通知内容
    private String title;
    // 通知标题
    private String content;
    // 通知内容
       private String action;
    // "APPLY" 或 "AGREE" 或 "REJECT"

    // 推送状态 0-未推送，1-推送成功，2-推送失败
    private Integer pushStatus;
    // 重试次数
    private Integer retryCount;

    private LocalDateTime createTime;
    private Boolean isRead;
    private LocalDateTime readTime;
    // 是否已读（站内信用）
}