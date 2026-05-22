package com.mall.demo.module.Refund.controller;

import com.mall.demo.common.result.RestResp;
import com.mall.demo.module.Refund.dto.req.ApplyRefundReqDto;
import com.mall.demo.module.Refund.dto.req.HandleRefundReqDto;
import com.mall.demo.module.Refund.dto.resp.ApplyRefundRespDto;
import com.mall.demo.module.Refund.dto.resp.HandleRefundRespDto;
import com.mall.demo.module.Refund.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/refund")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    /**
     * 用户发起退款申请
     */
    @PostMapping("/apply")
    public RestResp<ApplyRefundRespDto> apply(@Valid @RequestBody ApplyRefundReqDto reqDto) {
        ApplyRefundRespDto resp = refundService.applyRefund(reqDto);
        return RestResp.ok("退款申请已提交", resp);
    }

    /**
     * 商家审核退款
     * @param refundId 路径变量，明确操作对象
     */
    @PostMapping("/handle/{refundId}")
    public RestResp<HandleRefundRespDto> handle(@PathVariable String refundId,
                                                @RequestBody HandleRefundReqDto reqDto) {
        // 确保路径中的ID与DTO一致
        reqDto.setRefundId(refundId);
        HandleRefundRespDto resp = refundService.handleRefund(reqDto);

        String msg = reqDto.getIsAgree() ? "已同意退款" : "已拒绝退款";
        return RestResp.ok(msg, resp);
    }
}