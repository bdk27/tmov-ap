package com.brian.tmov.exception;

/**
 * @param status HTTP 狀態碼 (例如 400, 500)
 * @param code 業務錯誤代碼 (給前端查表)
 * @param message 詳細錯誤訊息 (給開發者除錯)
 * @param timestamp 錯誤發生時間
 */
public record ErrorResponse(

        int status,

        String code,

        String message,

        long timestamp
) {
    public ErrorResponse(int status, String code, String message) {
        this(status, code, message, System.currentTimeMillis());
    }
}
