package com.mall.demo.module.order.dto.resp;

import java.util.List;

import com.mall.demo.module.order.entity.Suborder;
import lombok.Data;

@Data
public class GetSuborderListRespDto {

    /**
     * 获取订单列表响应 DTO
     */


    private List<Suborder> suborderList;


}
