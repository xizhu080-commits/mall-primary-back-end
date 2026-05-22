package com.mall.demo.module.TransactionRecord.controller;

import com.mall.demo.common.result.PageResp;
import com.mall.demo.common.result.RestResp;
import com.mall.demo.module.TransactionRecord.dto.req.GetRecordsPageReqDto;
import com.mall.demo.module.TransactionRecord.dto.resp.GetRecordsPageRespDto;
import com.mall.demo.module.TransactionRecord.service.TransactionRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/transaction-record")
@Tag(name = "交易记录管理", description = "用户交易记录查询接口")
public class TransactionRecordController {

    private final TransactionRecordService transactionRecordService;

    /**
     * 分页查询交易记录
     * @param dto 分页参数
     * @return 交易记录分页数据
     */
    @Operation(summary = "分页查询交易记录")
    @GetMapping("/get-records-page")
    public RestResp<PageResp<GetRecordsPageRespDto>> getRecordsPage(@ModelAttribute GetRecordsPageReqDto dto) {
        PageResp<GetRecordsPageRespDto> result = transactionRecordService.getRecordsPage(dto);
        return RestResp.ok(result);
    }
}