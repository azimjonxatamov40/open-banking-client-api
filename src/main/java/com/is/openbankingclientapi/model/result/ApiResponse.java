package com.is.openbankingclientapi.model.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private final T data;
    private final Boolean success;
    private final ErrorInfo error;
    @JsonIgnore
    private final int httpStatus;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, true, null, 200);
    }

    public static <T> ApiResponse<T> error(ErrorCode code, String cause, List<String> fields) {
        ErrorInfo errorInfo = new ErrorInfo(code.getCode(), code.name(), cause, fields);
        return new ApiResponse<>(null, false, errorInfo, code.getHttpStatus());
    }

    public static <T> ApiResponse<T> error(ErrorCode code, List<String> fields) {
        return error(code, code.getCause(), fields);
    }
}
