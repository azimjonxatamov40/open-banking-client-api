package com.is.openbankingclientapi.model.account;

import lombok.Builder;
import lombok.Getter;

// 4.2 va 4.3 javobidagi hisobning to'liq ma'lumoti (account jadvalidan, AccountService.ACCOUNT_SQL orqali)
@Getter
@Builder
public class AccountDetailInfo {
    private String requestId;        // so'rovdagi requestId
    private String ban;              // hisob raqami (account.id)
    private String mfo;              // filial kodi (account.branch)
    private Long currentBalance;     // joriy operatsion kundagi qoldiq
    private String accountName;      // hisob nomi
    private String activeType;       // ACTIVE / PASSIVE / OTHER
    private String state;            // OPEN / CLOSED / NOT_DETERMINED / OTHER
    private String accountClassType; // ASSETS / LIABILITIES / EQUITY / INCOME / EXPENSES / CONTINGENT
    private String accountType;      // har doim ACCOUNT_WITHOUT_OVERDRAFT
    private String currency;         // valyuta kodi; so'm uchun 860
}
