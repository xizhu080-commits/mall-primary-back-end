package com.mall.demo.module.messageRecord.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@TableName("user_session")
public class UserSession {

    /**
     * 主键ID（自增或雪花ID）
     */
    @TableId(type = IdType.AUTO)  // 或 IdType.ASSIGN_ID
    private Long id;

    /**
     * 用户ID（这条记录属于谁）
     */
    private String userId;

    /**
     * 会话标识（A_B 格式）
     */
    private String sessionId;

    /**
     * 对方ID
     */
    private String partnerId;

    /**
     * 对方类型（user/shop/robot）
     */
    private String partnerType;

    /**
     * 对方昵称
     */
    private String partnerName;

    /**
     * 对方头像URL
     */
    private String partnerAvatar;

    /**
     * 最后一条消息内容
     */
    private String lastMessage;

    /**
     * 最后一条消息时间
     */
    private LocalDateTime lastMessageTime;

    /**
     * 未读消息数量
     */
    private Integer unreadCount;

 /*   *//**
     * 是否置顶
     *//*
    private Boolean isTop;

    *//**
     * 是否免打扰
     *//*
    private Boolean isDisturb;

    *//**
     * 创建时间
     *//*
    private LocalDateTime createTime;

    *//**
     * 更新时间
     *//*
    private LocalDateTime updateTime;*/
}