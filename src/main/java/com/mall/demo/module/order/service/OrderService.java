package com.mall.demo.module.order.service;


import com.mall.demo.common.result.PageResp;
import com.mall.demo.module.order.dto.req.*;
import com.mall.demo.module.order.dto.resp.*;

import java.util.List;

public interface OrderService {



    /*
    * 预览订单
    * */
   OrderPreviewRespDto previewOrder(OrderPreviewReqDto dto);


    /**
     * 创建订单
     */
    OrderCreateRespDto createOrder(OrderCreateReqDto dto);


    /*
    * 获取子订单详情
    * */
    GetSuborderDetailRespDto getSuborderDetail(String suborderId);


    /*
    * 获取订单列表
    * */
    GetSuborderListRespDto getSuborderList();





}