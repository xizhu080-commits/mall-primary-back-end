package com.mall.demo.module.Refund.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RefundNotifyFallback {

    @TableId(type = IdType.AUTO)
    private long id;

    private String messageJson;
    private Integer retryCount;

    private LocalDateTime nextRetryTime;
    private Integer status;
    private LocalDateTime createTime;

}
