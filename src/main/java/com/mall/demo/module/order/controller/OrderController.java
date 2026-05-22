package com.mall.demo.module.order.controller;


import com.mall.demo.common.result.RestResp;
import com.mall.demo.module.order.dto.req.*;
import com.mall.demo.module.order.dto.resp.GetSuborderDetailRespDto;
import com.mall.demo.module.order.dto.resp.GetSuborderListRespDto;
import com.mall.demo.module.order.dto.resp.OrderCreateRespDto;
import com.mall.demo.module.order.dto.resp.OrderPreviewRespDto;
import com.mall.demo.module.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    /*
    * 创建订单
    * */
    @PostMapping("/create")
    public RestResp<OrderCreateRespDto> createOrder(@RequestBody @Valid OrderCreateReqDto dto) {
        return RestResp.ok(orderService.createOrder(dto));
    }

    /**
     * 预览订单
     * @param dto 订单预览请求参数
     * @return 订单预览响应
     */
    @PostMapping("/preview")
    public RestResp<OrderPreviewRespDto> previewOrder(@RequestBody OrderPreviewReqDto dto) {
        return RestResp.ok(orderService.previewOrder(dto));
    }


    /*
    * 获取子订单详情
    * */
    @GetMapping("/suborder/detail")
    public RestResp<GetSuborderDetailRespDto> getSuborderDetail(@RequestParam String suborderId ) {
        return RestResp.ok(orderService.getSuborderDetail(suborderId));
    }


    /*
    * 获取订单列表
    * */
    @GetMapping("/suborder/list")
    public RestResp<GetSuborderListRespDto> getSuborderList( ) {
        return RestResp.ok(orderService.getSuborderList());
    }
}
