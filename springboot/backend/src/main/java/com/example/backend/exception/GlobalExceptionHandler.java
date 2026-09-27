package com.example.backend.exception;

import com.example.backend.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理：Controller / Service 抛出的异常统一转成 JSON 响应。
 *
 * <p>错误契约（与前端 utils/request.js 对齐）：</p>
 * <ul>
 *   <li>HTTP 状态码与业务 code 保持一致（400 / 401 / 403 / 404 / 405 / 409 / 500），
 *       前端可直接按 HTTP 状态码分流，不再需要「HTTP 200 但 body.code=500」这种双轨判断；</li>
 *   <li>响应体始终是 {@link Result}，message 为可直接展示给用户的中文提示。</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        Integer code = e.getCode() == null ? 400 : e.getCode();
        log.warn("业务异常 code={} message={}", code, e.getMessage());
        return ResponseEntity.status(toHttpStatus(code)).body(Result.error(code, e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        return badRequest("请求参数格式错误");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        return badRequest("缺少必要参数：" + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return badRequest("参数类型不正确：" + e.getName());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() == null
                ? "请求参数校验失败"
                : e.getBindingResult().getFieldError().getDefaultMessage();
        return badRequest(message);
    }

    /**
     * 请求方法不支持（例如对只提供 POST/PUT 的路径发起 GET）。
     * 不处理会落到兜底分支返回 500，语义不对。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.error(405, "请求方法不支持：" + e.getMethod()));
    }

    /**
     * 数据库唯一索引冲突（极端并发下两人同时改成同一用户名时的最后防线）。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一约束冲突: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Result.error(409, "该用户名已被使用，请换一个"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("服务器内部错误", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "服务器内部错误"));
    }

    private ResponseEntity<Result<Void>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(400, message));
    }

    /** 业务 code -> HTTP 状态码；未知 code 统一按 400 处理 */
    private HttpStatus toHttpStatus(Integer code) {
        switch (code) {
            case 401:
                return HttpStatus.UNAUTHORIZED;
            case 403:
                return HttpStatus.FORBIDDEN;
            case 404:
                return HttpStatus.NOT_FOUND;
            case 405:
                return HttpStatus.METHOD_NOT_ALLOWED;
            case 409:
                return HttpStatus.CONFLICT;
            case 500:
                return HttpStatus.INTERNAL_SERVER_ERROR;
            case 502:
                return HttpStatus.BAD_GATEWAY;
            case 503:
                return HttpStatus.SERVICE_UNAVAILABLE;
            case 504:
                return HttpStatus.GATEWAY_TIMEOUT;
            default:
                return HttpStatus.BAD_REQUEST;
        }
    }
}
