package com.mall.demo.module.product.dto.req;

import lombok.Data;

/**
 * 商品搜索请求 DTO
 */
@Data
public class ProductPageSearchReqDto {
    private String keyword;
    // 模糊查询关键字
    private String categoryId;


    //每页的数量
    private Integer pageNo = 1;
    private Integer pageSize = 20;
}