package com.is.openbankingclientapi.model.client;

import lombok.Builder;
import lombok.Getter;

// 3.1 javobidagi mijoz ma'lumotlari (client_p jadvalidan).
// @Builder — Lombok: ClientInfo.builder().mfo(...).build() ko'rinishida obyekt yaratish imkonini beradi.
// Setter yo'q — obyekt yaratilgandan keyin o'zgarmaydi; @Getter Jackson JSON'ga o'girishi uchun kerak.
@Getter
@Builder
public class ClientInfo {
    private String requestId;        // so'rovdagi requestId qaytariladi
    private String mfo;              // mijoz ochilgan filial kodi (branch)
    private String pinfl;            // JSHSHIR
    private String documentId;       // pasport seriyasi + raqami; bo'lmasa bo'sh satr
    private String phoneNumber;      // mobil telefon (phone_mobile)
    private String bankClientNumber; // mijozning bankdagi ichki raqami (client_p.id)
    private String firstName;        // ismi
    private String lastName;         // familiyasi (family)
    private String patronymic;       // otasining ismi
    private String birthDate;        // tug'ilgan sana, yyyy-mm-dd
}
