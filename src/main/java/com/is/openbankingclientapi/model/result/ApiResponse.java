package com.is.openbankingclientapi.model.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

// Barcha endpointlar uchun umumiy javob konverti: {data, success, error}.
// Obyekt to'g'ridan-to'g'ri emas, pastdagi ok(...) / error(...) statik metodlari orqali yaratiladi.
@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    private final T data;          // muvaffaqiyatli natija; xato bo'lsa null
    private final Boolean success; // true — muvaffaqiyatli, false — xato
    private final ErrorInfo error; // xato tafsiloti; muvaffaqiyatli bo'lsa null

    // Javob qaysi HTTP status bilan yuborilishi. @JsonIgnore — JSON'ga chiqmaydi, faqat controller ishlatadi
    @JsonIgnore
    private final int httpStatus;

    // Muvaffaqiyatli javob
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, true, null, 200);
    }

    // Xato javobi; sabab matni ErrorCode'dagi standart matndan olinadi
    public static <T> ApiResponse<T> error(ErrorCode code, List<String> fields) {
        return error(code, code.getCause(), fields);
    }


    // Xato javobi, sabab matni alohida beriladi (masalan ABS_ERROR'da bazaning xato xabari qo'shiladi).
    // HTTP status ErrorCode'dan olinadi.
    public static <T> ApiResponse<T> error(ErrorCode code, String cause, List<String> fields) {
        ErrorInfo errorInfo = new ErrorInfo(code.getCode(), code.name(), cause, fields);
        return new ApiResponse<>(null, false, errorInfo, code.getHttpStatus());
    }
}
