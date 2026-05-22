package com.mall.demo.module.merchant.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "商家信息修改请求DTO")
public class MerchantUpdateReqDto {




    @Schema(description = "联系电话")
    private String phone;

    @Size(max = 200, message = "简介长度不能超过200字")
    @Schema(description = "商家简介/备注")
    private String intro;

}
