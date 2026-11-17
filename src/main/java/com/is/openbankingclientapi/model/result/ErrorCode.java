package com.is.openbankingclientapi.model.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

// API qaytarishi mumkin bo'lgan barcha xatolar ro'yxati.
// Har bir qiymat: (HTTP status, API xato kodi, sabab matni). Javobdagi "message" — enum nomining o'zi.
//   5xxx kodlar — so'rov yoki ma'lumot bilan bog'liq xatolar
//   9xxx kodlar — ABS (baza) tomonidagi xatolar
@Getter
@AllArgsConstructor
public enum ErrorCode {
    EMPTY_PARAM(400, 5001, "Отсутствует обязательный параметр."),      // majburiy parametr berilmagan
    INVALID_PARAM(400, 5002, "Некорректное значение параметра."),      // parametr qiymati noto'g'ri formatda
    ACC_NOT_FOUND(200, 5004, "Счет не найден."),                       // hisob topilmadi (HTTP 200, success=false)
    CLIENT_NOT_FOUND(200, 5006, "Клиент не найден."),                  // mijoz topilmadi (HTTP 200, success=false)
    ABS_ERROR(500, 9000, "АБС вернула ошибку"),                        // bazadagi kutilmagan xato
    ABS_BAD_GATEWAY(502, 9002, "Ошибка соединения с АБС."),            // bazaga ulanib bo'lmadi
    ABS_TIMEOUT(504, 9004, "Истекло время ожидания ответа от АБС.");   // baza javobi kutilgan vaqtda kelmadi

    private final int httpStatus; // javob yuboriladigan HTTP status
    private final int code;       // javobdagi error.code
    private final String cause;   // javobdagi error.cause (standart matn)
}
