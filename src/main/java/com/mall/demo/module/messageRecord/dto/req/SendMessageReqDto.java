package com.mall.demo.module.messageRecord.dto.req;


import lombok.Data;

@Data
public class SendMessageReqDto {

    private String messagePublisherType;

    private String targetUserId;

    private String targetUserType;

    private String content;






}
