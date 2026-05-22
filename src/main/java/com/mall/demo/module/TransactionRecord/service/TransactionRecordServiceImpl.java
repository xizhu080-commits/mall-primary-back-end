package com.mall.demo.module.TransactionRecord.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.demo.common.enums.ErrorCodeEnum;
import com.mall.demo.common.exception.BizException;
import com.mall.demo.common.result.PageResp;
import com.mall.demo.common.util.SecurityUtils;
import com.mall.demo.module.TransactionRecord.dto.req.GetRecordsPageReqDto;
import com.mall.demo.module.TransactionRecord.dto.resp.GetRecordsPageRespDto;
import com.mall.demo.module.TransactionRecord.entity.TransactionRecord;
import com.mall.demo.module.TransactionRecord.mapper.TransactionRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionRecordServiceImpl implements TransactionRecordService {

    private final TransactionRecordMapper transactionRecordMapper;



    @Override
    public PageResp<GetRecordsPageRespDto> getRecordsPage(GetRecordsPageReqDto reqDto) {



        String userId = SecurityUtils.getId();
        if (userId == null) {
            throw new BizException(ErrorCodeEnum.USER_NOT_EXIST.getCode(), "用户未登录或token无效");

        }

        Integer pageNum = reqDto.getPageNum() != null ? reqDto.getPageNum() : 1;
        Integer pageSize = reqDto.getPageSize() != null ? reqDto.getPageSize() : 10;

        LambdaQueryWrapper<TransactionRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TransactionRecord::getUserId, userId);
        queryWrapper.orderByDesc(TransactionRecord::getPayTime);

        Page<TransactionRecord> pageParam = new Page<>(pageNum, pageSize);
        Page<TransactionRecord> transactionRecordPage = transactionRecordMapper.selectPage(pageParam, queryWrapper);

        List<GetRecordsPageRespDto> dtoList = transactionRecordPage.getRecords().stream().map(record -> {
            GetRecordsPageRespDto respDto = new GetRecordsPageRespDto();
            BeanUtils.copyProperties(record, respDto);
            return respDto;
        }).collect(Collectors.toList());

        return PageResp.of(
                transactionRecordPage.getTotal(),
                transactionRecordPage.getSize(),
                transactionRecordPage.getCurrent(),
                dtoList
        );
    }
}
