package com.mall.demo.module.Refund.controller;

import com.mall.demo.common.result.RestResp;
import com.mall.demo.module.Refund.entity.Refund;
import com.mall.demo.module.Refund.mapper.RefundMapper;
import com.mall.demo.module.Refund.service.RefundNotifyService;
import com.mall.demo.module.order.entity.Suborder;
import com.mall.demo.module.order.mapper.SuborderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/refund/notify")
@RequiredArgsConstructor
@Slf4j
public class RefundNotifyController {

    private final RefundNotifyService refundNotifyService;
    private final RefundMapper refundMapper;
    private final SuborderMapper suborderMapper;

    /**
     * 用户申请退款 → 通知商家
     */
    @PostMapping("/applyRefund")
    public RestResp<Void> notifyMerchantForApply(
            @RequestParam @NotBlank(message = "退款单ID不能为空") String refundId) {

        // ✅ 从数据库查询退款单和子订单
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            return RestResp.fail(400, "退款单不存在");
        }

        Suborder suborder = suborderMapper.selectById(refund.getSuborderId());
        if (suborder == null) {
            return RestResp.fail(400, "子订单不存在");
        }

        String content = String.format("用户申请退款，退款金额：¥%s，原因：%s",
                refund.getRefundAmount(), refund.getRefundReason());
        String title = "新的退款申请";

        refundNotifyService.notifyMerchantForApply(refund, suborder, content, title);
        return RestResp.ok("已通知商家处理退款", null);
    }

    /**
     * 商家处理退款 → 通知用户
     */
    @PostMapping("/handleRefund")
    public RestResp<Void> notifyUserForHandle(
            @RequestParam @NotBlank(message = "退款单ID不能为空") String refundId,
            @RequestParam Boolean isAgree,
            @RequestParam(required = false) String rejectReason) {

        // ✅ 从数据库查询退款单
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            return RestResp.fail(400, "退款单不存在");
        }

        String content;
        String title;
        if (isAgree) {
            content = String.format("您的退款申请已通过审核，退款金额 ¥%s 将在3-7个工作日内原路返回", refund.getRefundAmount());
            title = "退款申请已通过";
        } else {
            content = String.format("您的退款申请已被拒绝，原因：%s", rejectReason);
            title = "退款申请被拒绝";
        }

        refundNotifyService.notifyUserForHandle(refund, isAgree, rejectReason, content, title);
        return RestResp.ok("已通知用户退款结果", null);
    }
}