package com.is.openbankingclientapi.component;

import com.is.openbankingclientapi.model.ApiRequest;
import com.is.openbankingclientapi.model.result.ApiException;
import com.is.openbankingclientapi.model.result.ApiResponse;
import com.is.openbankingclientapi.model.result.ErrorCode;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CheckRequest {

    public void checkHeader(ApiRequest<?> request) {
        checkNotEmpty("requestId", request.getRequestId(),
                "dsign", request.getDsign(),
                "payload", request.getPayload());
    }

    public void checkNotEmpty(Object... namesAndValues) {
        List<String> emptyFields = new ArrayList<>();

        for (int i = 0; i < namesAndValues.length; i += 2) {
            Object value = namesAndValues[i + 1];
            if (value == null || value.toString().isEmpty()) {
                emptyFields.add(namesAndValues[i].toString());
            }
        }

        if (!emptyFields.isEmpty()) {
            throw new ApiException(ErrorCode.EMPTY_PARAM, emptyFields.toArray(new String[0]));
        }
    }

    public boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    public <T> ApiResponse<T> absError(Exception e) {
        if (e instanceof CannotGetJdbcConnectionException) {
            return ApiResponse.error(ErrorCode.ABS_BAD_GATEWAY, null);
        }
        if (e instanceof QueryTimeoutException) {
            return ApiResponse.error(ErrorCode.ABS_TIMEOUT, null);
        }
        return ApiResponse.error(ErrorCode.ABS_ERROR, ErrorCode.ABS_ERROR.getCause() + ": " + e.getMessage(), null);
    }
}
