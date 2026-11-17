package com.is.openbankingclientapi.model.account;

import lombok.Builder;
import lombok.Getter;

// 4.1 javobining elementi: hisobning qisqa ma'lumoti (AccountDetailInfo'ning qoldiqqa tegishli qismi)
@Getter
@Builder
public class AccountBaseInfo {
    private String requestId;    // so'rovdagi requestId
    private String ban;          // hisob raqami
    private String mfo;          // filial kodi
    private Long currentBalance; // joriy operatsion kundagi qoldiq (kernel.GetSOut natijasi)
    private String accountName;  // hisob nomi
}
