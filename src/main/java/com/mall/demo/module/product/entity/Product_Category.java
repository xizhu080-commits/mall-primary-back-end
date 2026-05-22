package com.mall.demo.module.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("product_category")
//分类属性表
public class Product_Category {
    @TableId(type = IdType.AUTO)
    private long id;


    //分类 ID
    private String categoryId;
    // 关联分类，如“手机类”
    private String categoryName;
    // 分类名称，如“手机”、“电脑”
    private String specName;
    // 规格名称，如“容量”、“颜色”、“尺码”
    private Integer sort;
    // 排序，决定前端先显示哪个
}

