package com.mall.demo.module.logistic.entity;

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
@TableName("logistic_notify_message")
public class LogisticNotifyMessage {

    @TableId(type = IdType.INPUT)  // notify_id 是主键且有值，使用 INPUT
    private String notifyId;

    private String logisticId;
    private String targetUserId;
    private String targetUserType;
    private String title;
    private String content;
    private String action;
    // "SIGN", "AGREE", "REJECT"
    private Integer pushStatus;
    // 0-待推送, 1-推送成功, 2-推送失败
    private Integer retryCount;
    private LocalDateTime createTime;
    private Boolean isRead;
    private LocalDateTime readTime;
    private LocalDateTime updateTime;
}