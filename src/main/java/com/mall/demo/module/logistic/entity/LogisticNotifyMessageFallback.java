package com.mall.demo.module.logistic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("logistic_notify_fallback")
public class LogisticNotifyMessageFallback {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String messageJson;
    private Integer retryCount;
    private LocalDateTime nextRetryTime;
    private Integer status;
    // 0-待重试, 1-成功, 2-失败
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}