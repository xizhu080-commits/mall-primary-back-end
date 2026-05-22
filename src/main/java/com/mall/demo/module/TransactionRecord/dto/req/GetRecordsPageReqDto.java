package com.mall.demo.module.TransactionRecord.dto.req;

import lombok.Data;

@Data
public class GetRecordsPageReqDto {
    private Integer pageNum;
    private Integer pageSize;

}
