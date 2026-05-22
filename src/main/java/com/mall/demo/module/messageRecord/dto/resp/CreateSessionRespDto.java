package com.mall.demo.module.messageRecord.dto.resp;

import lombok.Data;

@Data
public class CreateSessionRespDto {

    private String currentUserId;

    private String sessionId;

    private String partnerId;

    private String partnerName;

    private String partnerAvatar;

    private String partnerType;

    private boolean exist;
}
