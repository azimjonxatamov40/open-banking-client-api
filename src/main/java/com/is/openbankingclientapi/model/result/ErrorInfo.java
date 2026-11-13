package com.is.openbankingclientapi.model.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ErrorInfo {
    private Integer code;
    private String message;
    private String cause;
    private List<String> fields;
}
