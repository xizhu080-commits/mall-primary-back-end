package com.mall.demo.module.logistic.controller;

import com.mall.demo.common.result.RestResp;
import com.mall.demo.module.logistic.entity.Logistic;
import com.mall.demo.module.logistic.mapper.LogisticMapper;
import com.mall.demo.module.logistic.service.LogisticNotifyService;
import com.mall.demo.module.order.entity.Suborder;
import com.mall.demo.module.order.mapper.SuborderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/logistic/notify")
@RequiredArgsConstructor
@Slf4j
public class LogisticNotifyController {

    private final LogisticNotifyService logisticNotifyService;
    private final LogisticMapper logisticMapper;
    private final SuborderMapper suborderMapper;

    /**
     * 物流到达 → 通知用户签收确认
     */
    @PostMapping("/signPackage")
    public RestResp<Void> notifyUserForSign(
            @RequestParam @NotBlank(message = "物流单ID不能为空") String logisticId) {

        // ✅ 从数据库查询物流单
        Logistic logistic = logisticMapper.selectById(logisticId);
        if (logistic == null) {
            return RestResp.fail(400, "物流单不存在");
        }

        String content = String.format("您的包裹已到达配送站，请及时签收。快递单号：%s", logistic.getLogisticCompanyId());

        logisticNotifyService.notifyUserForSign(logisticId, logistic.getConsigneeId(), content);
        return RestResp.ok("已通知用户签收", null);
    }

    /**
     * 用户处理物流（签收/拒签）→ 通知商家
     */
    @PostMapping("/handlePackage")
    public RestResp<Void> notifyMerchantForHandle(
            @RequestParam @NotBlank(message = "物流单ID不能为空") String logisticId,
            @RequestParam Boolean isAgree,
            @RequestParam(required = false) String rejectReason) {

        // ✅ 从数据库查询物流单
        Logistic logistic = logisticMapper.selectById(logisticId);
        if (logistic == null) {
            return RestResp.fail(400, "物流单不存在");
        }

        String content;
        if (isAgree) {
            content = String.format("用户已确认签收包裹，快递单号：%s", logistic.getLogisticCompanyId());
        } else {
            content = String.format("用户拒绝签收包裹，原因：%s。快递单号：%s", rejectReason, logistic.getLogisticCompanyId());
        }

        logisticNotifyService.notifyMerchantForHandle(logistic, isAgree, rejectReason, content);
        return RestResp.ok("已通知商家处理结果", null);
    }

    /**
     * 商家发货 → 通知用户
     */
    @PostMapping("/shipPackage")
    public RestResp<Void> notifyUserForShipment(
            @RequestParam @NotBlank(message = "物流单ID不能为空") String logisticId) {

        // ✅ 从数据库查询物流单和子订单
        Logistic logistic = logisticMapper.selectById(logisticId);
        if (logistic == null) {
            return RestResp.fail(400, "物流单不存在");
        }

        Suborder suborder = suborderMapper.selectById(logistic.getSuborderId());
        if (suborder == null) {
            return RestResp.fail(400, "子订单不存在");
        }

        String content = String.format("商家已发货，快递单号：%s，预计3天内送达", logistic.getLogisticCompanyId());

        logisticNotifyService.notifyUserForHandle(logistic, suborder.getUserId(), content);
        return RestResp.ok("已通知用户发货信息", null);
    }
}