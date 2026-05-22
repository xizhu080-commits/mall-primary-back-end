package com.mall.demo.module.messageRecord.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@TableName("payment_notify_fallback")
public class MessageRecordFallback {

    @TableId(type = IdType.AUTO)
    private long id;

    private String messageJson;

    private Integer retryCount;

    private LocalDateTime nextRetryTime;

    //0-待重试,1-成功,2-失败
    private Integer status;

    private LocalDateTime createTime;


}
