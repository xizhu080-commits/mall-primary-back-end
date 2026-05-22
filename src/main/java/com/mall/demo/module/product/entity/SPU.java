package com.mall.demo.module.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("spu")
//主表：商品表
public class SPU {


    @TableId(type = IdType.ASSIGN_ID)
    // 商品 ID
    private String spuId;
    private String shopId;


    // 比如：iPhone 15
    private String productName;

    // 关联“手机类”  分类 ID
    private String categoryId;

    // 商品图片
    @TableField("productUrl")
    private String productUrl;

    // 商品价格
    private BigDecimal price;

    //'商品状态：0-下架，1-上架 , 2-预售',
    private  Integer status;

    //'活动类型: 0-普通订单；1-秒杀订单；2-团购订单；3-促销订单；4-预售订单'
    private  Integer activeType;
    // 时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;


}
