package com.is.openbankingclientapi.component;

import com.is.openbankingclientapi.model.ApiRequest;
import com.is.openbankingclientapi.model.result.ApiException;
import com.is.openbankingclientapi.model.result.ApiResponse;
import com.is.openbankingclientapi.model.result.ErrorCode;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// Barcha servislar uchun umumiy yordamchi: so'rov maydonlarini tekshiradi va
// baza (ABS) xatolarini standart API javobiga aylantiradi.
@Component
public class CheckRequest {

    // Har bir so'rovda bo'lishi shart bo'lgan umumiy maydonlarni tekshiradi: requestId, dsign, payload.
    // Bittasi ham bo'sh bo'lsa, EMPTY_PARAM xatosi tashlanadi.
    public void checkHeader(ApiRequest<?> request) {
        checkNotEmpty("requestId", request.getRequestId(),
                "dsign", request.getDsign(),
                "payload", request.getPayload());
    }

    // Argumentlar juft-juft beriladi: (nom1, qiymat1, nom2, qiymat2, ...).
    // Qiymati null yoki bo'sh satr bo'lgan BARCHA maydonlar nomi yig'iladi va bitta EMPTY_PARAM
    // xatosida qaytariladi — mijoz yetishmayotgan maydonlarning hammasini birdaniga ko'radi.
    public void checkNotEmpty(Object... namesAndValues) {
        List<String> emptyFields = new ArrayList<>();

        // i — maydon nomi, i + 1 — uning qiymati, shuning uchun qadam 2
        for (int i = 0; i < namesAndValues.length; i += 2) {
            Object value = namesAndValues[i + 1];
            if (value == null || value.toString().isEmpty()) {
                emptyFields.add(namesAndValues[i].toString());
            }
        }

        if (!emptyFields.isEmpty()) {
            throw new ApiException(ErrorCode.EMPTY_PARAM, emptyFields.toArray(new String[0]));
        }
    }

    // Satr null yoki bo'sh ekanini tekshiradi (ixtiyoriy maydonlar uchun, xato tashlamaydi)
    public boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    // Servislardagi kutilmagan xatolarni (asosan baza bilan ishlashdagi) API xato kodiga o'giradi:
    //  - bazaga ulanib bo'lmadi        -> ABS_BAD_GATEWAY (502)
    //  - so'rov vaqti tugadi           -> ABS_TIMEOUT (504)
    //  - qolgan hamma narsa            -> ABS_ERROR (500), sabab matniga xatoning o'z xabari qo'shiladi
    public <T> ApiResponse<T> absError(Exception e) {
        if (e instanceof CannotGetJdbcConnectionException) {
            return ApiResponse.error(ErrorCode.ABS_BAD_GATEWAY, null);
        }
        if (e instanceof QueryTimeoutException) {
            return ApiResponse.error(ErrorCode.ABS_TIMEOUT, null);
        }

        return ApiResponse.error(ErrorCode.ABS_ERROR, ErrorCode.ABS_ERROR.getCause() + ": " + e.getMessage(), null);
    }
}
