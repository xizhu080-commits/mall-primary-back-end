package com.mall.demo.module.coupon.entity;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("coupon_receive_record")
public class CouponReceiveRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("template_id")
    private String templateId;

    @TableField("user_id")
    private String userId;

    @TableField("receive_count")
    private Integer receiveCount;

    @TableField("status")
    private Integer status;
    // 0未领够 1已领够

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}