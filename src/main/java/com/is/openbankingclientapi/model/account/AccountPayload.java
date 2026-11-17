package com.is.openbankingclientapi.model.account;

import lombok.Getter;
import lombok.Setter;

// Bitta hisobni ko'rsatuvchi juftlik. 4.3 (/accounts/details) va 4.4 (/accounts/exists) payload'i,
// shuningdek 4.1 (/accounts/balances) ro'yxatining elementi.
@Getter
@Setter
public class AccountPayload {
    private String ban; // hisob raqami (account.id)
    private String mfo; // filial kodi (account.branch)
}
