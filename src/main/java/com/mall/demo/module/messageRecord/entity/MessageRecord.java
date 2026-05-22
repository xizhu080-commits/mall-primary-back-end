package com.mall.demo.module.messageRecord.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@TableName("message_record")
public class MessageRecord {


    private String messageRecordId;

    private String messagePublisherId;
    private String messagePublisherType;
    // 目标用户（商家ID 或 用户ID）
    private String targetUserId;


    private String targetUserType;
    // "MERCHANT" 或 "USER"


    // 通知标题
    private String content;
    // 通知内容



    // 推送状态'推送状态: 0-待推送,1-推送成功,2-推送失败'
    private Integer pushStatus;

    // 重试次数
    private Integer retryCount;

    //会话双方ID
    private String sessionId;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean isRead;
    private LocalDateTime readTime;
    // 是否已读（站内信用）
}
