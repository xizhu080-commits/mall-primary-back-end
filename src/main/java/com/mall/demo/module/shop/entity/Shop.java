package com.mall.demo.module.shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("shop")
public class Shop {


    @TableId(type = IdType.ASSIGN_ID)
    private String shopId;

    private String merchantId;





    private String shopName;
    // 店名/笔名
    private String shopLogo;


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