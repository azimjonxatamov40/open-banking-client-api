package com.is.openbankingclientapi.model.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

// Javobdagi "error" obyekti. HTTP status 200 bo'lmaganda javob tanasi faqat shu obyektdan iborat bo'ladi.
@Getter
@AllArgsConstructor
public class ErrorInfo {
    private Integer code;        // API xato kodi (ErrorCode.code), masalan 5001
    private String message;      // xato nomi (ErrorCode enum nomi), masalan EMPTY_PARAM
    private String cause;        // sabab matni
    private List<String> fields; // xatoga aloqador maydonlar yoki qiymatlar; bo'lmasa null
}
