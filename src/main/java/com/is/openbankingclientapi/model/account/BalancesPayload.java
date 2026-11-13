package com.is.openbankingclientapi.model.account;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BalancesPayload {
    private List<AccountPayload> bans;
}
