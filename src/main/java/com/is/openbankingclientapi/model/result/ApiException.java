package com.is.openbankingclientapi.model.result;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public class ApiException extends RuntimeException {
    private final ErrorCode code;
    private final List<String> fields;

    public ApiException(ErrorCode code, String... fields) {
        super(code.name());
        this.code = code;
        this.fields = Arrays.asList(fields);
    }
}
