package com.mall.demo.common.result;

import lombok.Data;
import java.io.Serializable;

/**
 * Service层业务响应类
 */
@Data
public class RestResp<T> implements Serializable {

    private int code;
    // 业务自定义状态码（非HTTP状态码）
    private String msg;
    // 提示信息
    private T data;
    // 具体的业务数据




    private RestResp(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // 成功返回
    public static <T> RestResp<T> ok(T data) {
        return new RestResp<>(200, "success", data);
    }

    public static <T> RestResp<T> ok() {
        return ok(null);
    }

    public static <T> RestResp<T> ok(String msg, T data) {
        return new RestResp<>(200, msg, data);
    }


    // 业务逻辑失败返回
    public static <T> RestResp<T> fail(int code, String msg) {
        return new RestResp<>(code, msg, null);
    }
}