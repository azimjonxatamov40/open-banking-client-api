package com.is.openbankingclientapi.model.account;

import lombok.Getter;
import lombok.Setter;

// 4.2 (/accounts/search) so'rovining payload qismi.
// mfo va bankClientNumber majburiy; balanceType, ccy, order filtrlaridan kamida bittasi berilishi shart.
@Getter
@Setter
public class SearchPayload {
    private String mfo;              // filial kodi (account.branch) — majburiy
    private String balanceType;      // balans hisobi (account.acc_bal) — filtr
    private String ccy;              // valyuta kodi (account.currency) — filtr
    private String order;            // hisobning tartib raqami (account.id_order) — filtr
    private String bankClientNumber; // mijozning bankdagi raqami (account.client) — majburiy
}
