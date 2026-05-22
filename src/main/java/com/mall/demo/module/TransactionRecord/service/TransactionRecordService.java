package com.mall.demo.module.TransactionRecord.service;


import com.mall.demo.common.result.PageResp;
import com.mall.demo.module.TransactionRecord.dto.req.GetRecordsPageReqDto;
import com.mall.demo.module.TransactionRecord.dto.resp.GetRecordsPageRespDto;

public interface TransactionRecordService {


    PageResp<GetRecordsPageRespDto> getRecordsPage(GetRecordsPageReqDto dto);


}
