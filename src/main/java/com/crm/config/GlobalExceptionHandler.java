package com.crm.config;

import com.crm.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 统一拦截 Controller 层抛出的异常，转换为标准的 Result 格式返回。
 * 避免将异常堆栈直接暴露给前端，提升系统健壮性。
 *
 * @author zhujie
 */
@Slf4j
public class GlobalExceptionHandler {
    /**
     * 参数校验异常 - @Valid 校验请求体失败时触发
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数校验：{}", msg);
        return Result.fail(400,msg);
    }

    /**
     * 绑定异常 - 参数类型转换失败时触发
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handelBindException(BindException e) {
        String msg = e.getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数绑定失败：{}",msg);
        return Result.fail(400,msg);
    }

    /**
     * 约束违反异常 - @RequestParam / @PathVariable 校验失败时触发
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        log.warn("约束违反：{}",msg);
        return Result.fail(400,msg);
    }

    /**
     * 业务异常 - Service 层抛出的 RuntimeException
     * 使用 WARN 级别而非 ERROR, 因为这是”预期内“的异常（如客户不存在）
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.warn("业务异常：{}",e.getMessage());
        return Result.fail(400,e.getMessage());
    }

    /**
     * 兜底异常 - 捕获所有未处理的异常，防止堆栈泄露给前端
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.error("服务器内部错误：" + e.getMessage());
    }

}
