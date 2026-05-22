package com.mall.demo.module.merchant.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "商家入驻请求对象")
public class MerchantRegisterReqDto {



    @NotBlank(message = "商家名称不能为空")
    @Schema(description = "商家名称")
    private String merchantName;


    @NotBlank(message = "联系电话不能为空")
    @Schema(description = "联系电话")
    private String phone;


    @NotBlank(message = "密码不能为空")
    private String password;
    @NotBlank(message = "确认密码不能为空")
    @Schema(description = "确认密码")
    private String confirmPassword;

    @Schema(description = "主营品牌不能为空")
    @NotBlank(message = "主营品牌不能为空")
    private String brand;

    @Schema(description = "验证码")
    private String code;

    @NotBlank(message = "真实姓名不能为空")
    @Schema(description = "真实姓名/法人姓名")
    private String realName;

    @NotBlank(message = "身份证号不能为空")
    @Schema(description = "身份证号")
    private String idCard;


    @NotBlank(message = "营业执照不能为空")
    @Schema(description = "营业执照")
    private String qualificationCertificate;


    // 新增简介字段
    @Schema(description = "商家简介/备注（如：主营玄幻小说创作）")
    private String intro;


}
