package com.mall.demo.common.enums;

public enum ErrorCodeEnum {



//一般异常
    OK(200, "成功"),
    PARAM_ERROR(400, "参数错误"),
    SYSTEM_ERROR(500, "系统错误"),
    MANY_REQUEST(600, "请求过于频繁"),
    SYSTEM_BUSY(700, "系统繁忙"),
    HEADER_MISSING(800, "请求头缺失"),
    TOKEN_ERROR(900, "token错误"),
    HEADER_ERROR(801, "请求头错误"),
    NOT_PERMISSION(901, "无权限"),
    UPDATE_ERROR(902, "更新失败"),


//手机
    PHONE_ERROR(10001, "手机号格式错误"),
        PHONE_NOT_EXIST(10002, "手机号不存在"),
            PHONE_EXIST(1003, "手机号已存在"),
                PHONE_LOCK_ACCOUNT(10004, "手机号被锁定"),


//    验证码
    CODE_ERROR(2001, "验证码错误"),
        CODE_EXPIRED(20002, "验证码已过期"),



//    密码
    PASSWORD_ERROR(3001, "密码错误"),
        PASSWORD_NOT_EXIST(3002, "密码格式错误"),
            PASSWORD_NOT_MATCH(3003, "密码不一致"),



//    用户
    USER_NOT_EXIST(4001, "用户不存在"),
    USER_LOCK_ACCOUNT(4002, "用户被锁定"),



//    商家

    MERCHANT_EXIST(5001, "商户已注册"),
    MERCHANT_NOT_EXIST(5002, "商户不存在"),
    INTRO_ERROR(5003, "简介长度不能超过200字"),


//    商品
    PRO_NOT_FOUND(6001, "商品不存在"),
    STOCK_NOT_ENOUGH(6002, "库存不足"),
    PRO_NOT_SELL(6003,"商品未上架" ),

//订单
    ORDER_NOT_EXIST(7001, "订单不存在"),
    ORDER_CLOSED(7002, "订单已关闭"),
    ORDER_NOT_PAY(7003, "订单未支付"),
    ORDER_NOT_SUPPORT_REFUND(7004, "订单不支持退款"),



//    权限
    NO_PERMISSION(8001, "没有权限"),




//    优惠卷
    USER_ALREADY_GET_COUPON(9001, "用户已领取优惠卷"),
    COUPON_NOT_ON_SALE(9002, "优惠卷未上架"),
    COUPON_EXPIRED(9003, "优惠卷已过期"),
    COUPON_SOLD_OUT(9004, "优惠卷已售罄"),



    //    店铺
    SHOP_EXIST(1001, "店铺已存在"),
    SHOP_NOT_EXIST(1002, "店铺不存在"),


//物流
    LOGISTIC_NOT_EXIST(1002, "物流不存在"),

    //支付
    PAYMENT_NOT_EXIST(1003, "支付单不存在"),
    PAYMENT_ALREADY_PAY(1004, "支付单已支付"),
    PAYMENT_CLOSED(1005, "支付单已关闭"),

    //地址
    ADDRESS_NOT_EXIST(2001, "地址不存在" );






    private final int code;
    private final String message;

    ErrorCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }
    public String getMessage() {
        return message;
    }
}
