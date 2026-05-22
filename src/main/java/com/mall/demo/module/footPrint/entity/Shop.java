package com.mall.demo.module.footPrint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_shop")
public class Shop {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long merchantId; // 关联 Merchant 的 userId

    private String shopName; // 店名/笔名
    private String shopLogo;
    private String shopIntro; // 店铺简介

    /**
     * 营业状态：1-正常营业，0-休息中
     */
    private Integer workStatus;

    /**
     * 店铺评分
     */
    private Double rating;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}