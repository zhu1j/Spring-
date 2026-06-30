package com.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    //HTTP 状态码
    private int code;
    //提示信息
    private String message;
    //响应数据
    private T data;
    //当前时间戳（毫秒）
    private long timestamp;

    //========静态工厂方法==========
    //成功（无数据）
    public static <T> Result<T> ok(T data){
        return Result.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    //成功（自定义消息 + 数据）
    public static <T> Result<T> ok(String message, T data){
        return Result.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    //失败（指定 code 和 message）
    public static <T> Result<T> fail(int code,String message){
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    //失败（默认 400 业务错误）
    public static <T> Result<T> fail(String message){
        return fail(400,message);  //自己调用自己
    }
    //服务器内部错误（500）
    public static <T> Result<T> error(String message){
        return fail(500,message);
    }
}
