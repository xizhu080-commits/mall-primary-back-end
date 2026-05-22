package com.mall.demo.module.user.dto.resp;

import com.mall.demo.module.Refund.entity.RefundNotifyMessage;
import com.mall.demo.module.logistic.entity.Address;
import com.mall.demo.module.logistic.entity.LogisticNotifyMessage;
import com.mall.demo.module.order.entity.Suborder;
import com.mall.demo.module.payment.entity.PaymentNotifyMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;


import java.time.LocalDateTime;
import java.util.List;


@Data
@Schema(description = "获取当前用户信息响应DTO")
public class GetUserInfoRespDto {

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "头像地址")
    private String userAvatar;

    @Schema(description = "手机号（脱敏处理，如138****0000）")
    private String phone;

    @Schema(description = "邮箱（脱敏处理，如ge****@gmail.com）")
    private String email;

    @Schema(description = "性别：0-未知，1-男，2-女")
    private Integer sex;

    @Schema(description = "用户角色：1-普通用户，2-作者，3-管理员")
    private Integer role;

    //订单数量,消息数量,优惠卷数量
    private Integer suborderCount;
    private Integer messageCount;
    private Integer couponCount;
    @Schema(description = "账号状态：1-正常，0-禁用")
    private Integer status;

    @Schema(description = "注册时间")
    private LocalDateTime createTime;


    //我的消息:退款消息
    private List<RefundNotifyMessage> myRefundMessageList;
    //我的消息:物流消息
    private List<LogisticNotifyMessage> myLogisticMessageList;
    //我的消息:支付消息
    private List<PaymentNotifyMessage> myPayMessageList;


}