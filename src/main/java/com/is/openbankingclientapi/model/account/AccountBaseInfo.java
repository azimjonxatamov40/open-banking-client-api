package com.is.openbankingclientapi.model.account;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountBaseInfo {
    private String requestId;
    private String ban;
    private String mfo;
    private Long currentBalance;
    private String accountName;
}
