package com.is.openbankingclientapi.model.result;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

// So'rovni bajarib bo'lmasligini bildiruvchi xato (bo'sh/noto'g'ri parametr, topilmagan mijoz yoki hisob).
// RuntimeException'dan meros olgan — metod sarlavhasida "throws" yozish shart emas.
// Servis qayerda tashlamasin, controller'dagi @ExceptionHandler ushlab, standart xato javobiga aylantiradi.
@Getter
public class ApiException extends RuntimeException {
    // Xato turi (HTTP status, kod va matnni o'zida saqlaydi)
    private final ErrorCode code;
    // Xatoga aloqador maydon nomlari yoki qiymatlar (masalan "payload.pinfl" yoki topilmagan hisob raqami)
    private final List<String> fields;

    // String... — maydonlarni vergul bilan ketma-ket berish mumkin, umuman bermasa ham bo'ladi
    public ApiException(ErrorCode code, String... fields) {
        super(code.name());
        this.code = code;
        this.fields = Arrays.asList(fields);
    }
}
