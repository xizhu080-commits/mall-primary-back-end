package com.mall.demo.module.order.dto.resp;

import com.mall.demo.module.order.entity.Suborder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderCreateRespDto {


    private String orderId;
    private String userId;


    //使用的优惠卷列表
    private List<String> usedCouponIds ;

    private List<Suborder> subOrderList;







}
