package com.is.openbankingclientapi.model.account;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountDetailInfo {
    private String requestId;
    private String ban;
    private String mfo;
    private Long currentBalance;
    private String accountName;
    private String activeType;
    private String state;
    private String accountClassType;
    private String accountType;
    private String currency;
}
