package com.is.openbankingclientapi.model.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    EMPTY_PARAM(400, 5001, "Отсутствует обязательный параметр."),
    INVALID_PARAM(400, 5002, "Некорректное значение параметра."),
    ACC_NOT_FOUND(200, 5004, "Счет не найден."),
    CLIENT_NOT_FOUND(200, 5006, "Клиент не найден."),
    ABS_ERROR(500, 9000, "АБС вернула ошибку"),
    ABS_BAD_GATEWAY(502, 9002, "Ошибка соединения с АБС."),
    ABS_TIMEOUT(504, 9004, "Истекло время ожидания ответа от АБС.");

    private final int httpStatus;
    private final int code;
    private final String cause;
}
