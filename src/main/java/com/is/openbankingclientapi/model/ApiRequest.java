package com.is.openbankingclientapi.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiRequest<T> {
    private String requestId;
    private String dsign;
    private T payload;
}
