package com.mall.demo.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@TableName("coupon_user")
public class CouponUser {



        @TableId(type = IdType.ASSIGN_ID)
        private String couponId;

        private String userId;
        private String templateId;

        /**
         * 0 未使用
         * 1 已使用
         * 2 已过期
         * 3 已失效
         */
        private Integer status;

        private LocalDateTime startTime;
        private LocalDateTime endTime;

        // 使用时间
        private LocalDateTime usedTime;

        /**
         * 过期后 +7天 删除
         */
        private LocalDateTime expireTime;

        private LocalDateTime createTime;
        private LocalDateTime updateTime;


    }

