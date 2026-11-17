package com.is.openbankingclientapi.model.account;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// 4.1 (/accounts/balances) so'rovining payload qismi
@Getter
@Setter
public class BalancesPayload {
    // Qoldig'i so'ralayotgan hisoblar ro'yxati; bo'sh bo'lmasligi kerak
    private List<AccountPayload> bans;
}
