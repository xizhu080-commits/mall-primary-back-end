package com.mall.demo.module.messageRecord.dto.req;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSessionReqDto {


    @NotBlank(message = "对方ID不能为空")
    private String partnerId;

    @NotBlank(message = "对方名称不能为空")
    private String partnerName;

    @NotBlank(message = "对方头像不能为空")
    private String partnerAvatar;

    @NotBlank(message = "对方类型不能为空")
    private String partnerType;


}
