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
@TableName("sku")
//子表：商品规格表
public class SKU {
    @TableId(type = IdType.ASSIGN_ID)
    private String skuId;

    private String shopId;

    // 商品 ID
    private String spuId;

    // 商品图片 URL
    @TableField("productUrl")
    private String productUrl;

    // 商品名称
    private String productName;

    //商品状态：1-上架 0-下架 2-预售
    private Integer status;

    /**
     * 规格数据 (JSON 格式)
     * 手机类存：{"容量": "128G", "颜色": "远峰蓝"}
     * 衣服类存：{"尺码": "XL", "材质": "纯棉"}
     */
    private String specData;

    private BigDecimal price;

    // 库存数量
    private Integer stock;



    // 时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expireTime;

}
