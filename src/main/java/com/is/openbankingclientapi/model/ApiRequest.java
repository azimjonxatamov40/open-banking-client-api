package com.is.openbankingclientapi.model;

import lombok.Getter;
import lombok.Setter;

// Barcha endpointlar uchun umumiy so'rov konverti. T — endpointga xos payload turi
// (ClientPayload, AccountPayload, BalancesPayload, SearchPayload).
// @Getter/@Setter — Lombok getter va setterlarni yaratadi; setterlar Jackson JSON'ni obyektga o'girishi uchun kerak.
@Getter
@Setter
public class ApiRequest<T> {
    // So'rovning noyob identifikatori; loglarda va javobda qaytariladi
    private String requestId;
    // Raqamli imzo. Hozircha faqat bo'sh emasligi tekshiriladi, imzoning o'zi tekshirilmaydi
    private String dsign;
    // So'rovning asosiy mazmuni
    private T payload;
}
