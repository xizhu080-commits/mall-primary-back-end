package com.mall.demo.module.logistic.controller;

import com.mall.demo.common.result.RestResp;
import com.mall.demo.module.logistic.dto.req.CreateLogisticReqDto;
import com.mall.demo.module.logistic.service.LogisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/logistic")
@RequiredArgsConstructor
public class LogisticController {

    private final LogisticService logisticService;

    /**
     * 商家发货/用户退货
     */
    @PostMapping("/create")
    public RestResp<Void> createLogistic(@Valid @RequestBody CreateLogisticReqDto reqDto) {
        logisticService.createLogistic(reqDto);
        return RestResp.ok();
    }

    /**
     * 用户/商家处理签收确认
     * @param logisticId 物流单ID
     * @param isAgree 是否同意签收 (true-签收, false-拒签)
     */
    @PostMapping("/handle/{logisticId}")
    public RestResp<Void> handleSign(@PathVariable String logisticId,
                                         @RequestParam boolean isAgree,
                                         @RequestParam(required = false) String rejectReason) {
        logisticService.handleSignLogistic(logisticId, isAgree, rejectReason);
        return RestResp.ok(isAgree ? "已确认签收" : "已拒签", null);
    }

    /**
     * 申请（催办）确认签收
     * 用于物流送达后手动通知对方
     */
    @PostMapping("/apply-sign/{suborderId}")
    public RestResp<Void> applySign(@PathVariable String suborderId, @RequestParam String logisticId) {
        logisticService.applySignLogistic(suborderId, logisticId);
        return RestResp.ok("已提醒收货人签收", null);
    }
}