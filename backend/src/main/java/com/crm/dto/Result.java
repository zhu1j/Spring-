package com.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应体
 * <p>
 * 所有接口遵循此格式，前端可统一解析。
 * 仿企业级项目标准 {code, message, data} 结构。
 *
 * @param <T> data 的泛型类型
 * @author CRM Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** HTTP 状态码，200 表示成功 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 当前时间戳 */
    private long timestamp;

    // ==================== 静态工厂方法 ====================

    /** 成功（无数据） */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /** 成功（携带数据） */
    public static <T> Result<T> ok(T data) {
        return Result.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /** 成功（自定义消息） */
    public static <T> Result<T> ok(String message, T data) {
        return Result.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /** 失败 */
    public static <T> Result<T> fail(int code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /** 失败（默认 400） */
    public static <T> Result<T> fail(String message) {
        return fail(400, message);
    }

    /** 服务器内部错误 */
    public static <T> Result<T> error(String message) {
        return fail(500, message);
    }
}
