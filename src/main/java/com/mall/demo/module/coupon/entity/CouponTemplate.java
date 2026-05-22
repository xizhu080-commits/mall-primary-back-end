package com.mall.demo.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@TableName("coupon_template")
public class CouponTemplate {



        @TableId(type = IdType.ASSIGN_ID)
        private String templateId;

        private String couponName;

        /**
         * 1 满减
         * 2 折扣
         */
        private Integer couponType;

        private BigDecimal thresholdAmount;
        private BigDecimal discountAmount;
        private BigDecimal discountRate;

        /**
         * 0 不可叠加
         * 1 可叠加
         */
        private Integer stackable;

        private LocalDateTime startTime;
        private LocalDateTime endTime;

        // 优惠券数量
        private Integer totalCount;
        // 剩余数量
        private Integer remainCount;

        /**
         *
         * 1 正常
         * 0 下架/未上架
         */
        private Integer status;


        /*
        * 1普通优惠卷
        * 2会员优惠卷
        * 3大促优惠卷(618 双11 双12 等)
        * */
        private Integer activeType;



        private LocalDateTime createTime;
        private LocalDateTime updateTime;

        /*
        * 每个用户可以领取几张优惠卷
        * */
        private Integer getLimit;
    }

