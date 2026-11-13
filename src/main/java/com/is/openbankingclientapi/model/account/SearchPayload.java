package com.is.openbankingclientapi.model.account;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchPayload {
    private String mfo;
    private String balanceType;
    private String ccy;
    private String order;
    private String bankClientNumber;
}
