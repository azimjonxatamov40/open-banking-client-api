package com.is.openbankingclientapi.service;

import com.is.openbankingclientapi.component.CheckRequest;
import com.is.openbankingclientapi.model.ApiRequest;
import com.is.openbankingclientapi.model.client.ClientInfo;
import com.is.openbankingclientapi.model.client.ClientPayload;
import com.is.openbankingclientapi.model.result.ApiException;
import com.is.openbankingclientapi.model.result.ApiResponse;
import com.is.openbankingclientapi.model.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicReference;

// Mijoz (jismoniy shaxs) ma'lumotlari bilan ishlaydigan servis. Ma'lumot ABS bazasidagi client_p jadvalidan olinadi.
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    // Spring tayyorlab beradigan JDBC yordamchisi: ulanish olish, so'rovni bajarish va yopishni o'zi qiladi
    private final JdbcTemplate jdbcTemplate;
    private final CheckRequest checkRequest;

    // 3.1 Mijozni JSHSHIR (pinfl) bo'yicha topib, uning ma'lumotlarini qaytaradi
    public ApiResponse<ClientInfo> getClientInfo(ApiRequest<ClientPayload> request) {
        try {
            // 1) Umumiy maydonlar (requestId, dsign, payload) va pinfl borligini tekshirish
            checkRequest.checkHeader(request);
            String pinfl = request.getPayload().getPinfl();
            checkRequest.checkNotEmpty("payload.pinfl", pinfl);

            // 2) Format tekshiruvi: pinfl aynan 14 ta raqamdan iborat bo'lishi kerak
            if (!pinfl.matches("\\d{14}")) {
                throw new ApiException(ErrorCode.INVALID_PARAM, "payload.pinfl");
            }

            // 3) Bazadan o'qish. Lambda ichidan tashqi o'zgaruvchiga qiymat yozib bo'lmaydi (u final bo'lishi shart),
            //    shuning uchun natija AtomicReference "quti"siga solinadi.
            //    Pasport seriyasi va raqami SQL'ning o'zida birlashtiriladi, tug'ilgan sana yyyy-mm-dd satriga o'giriladi.
            //    "?" o'rniga pinfl parametr sifatida beriladi (SQL injection'dan himoya).
            AtomicReference<ClientInfo> clientInfo = new AtomicReference<>();
            jdbcTemplate.query("select c.branch, \n" +
                    "       c.pinfl, \n" +
                    "       c.passport_serial || c.passport_number document_id, \n" +
                    "       c.phone_mobile, \n" +
                    "       c.id, \n" +
                    "       c.first_name, \n" +
                    "       c.family, \n" +
                    "       c.patronymic, \n" +
                    "       to_char(c.birthday, 'yyyy-mm-dd') birthday \n" +
                    "  from client_p c \n" +
                    " where c.pinfl = ? ", (ResultSet rs) -> {
                // Bu lambda har bir topilgan qator uchun bir marta chaqiriladi.
                // Bitta pinfl bo'yicha bir nechta qator chiqsa, faqat birinchisi olinadi.
                if (clientInfo.get() == null) {
                    clientInfo.set(ClientInfo.builder()
                            .requestId(request.getRequestId())
                            .mfo(rs.getString("branch"))
                            .pinfl(rs.getString("pinfl"))
                            .documentId(rs.getString("document_id") == null ? "" : rs.getString("document_id"))
                            .phoneNumber(rs.getString("phone_mobile"))
                            .bankClientNumber(rs.getString("id"))
                            .firstName(rs.getString("first_name"))
                            .lastName(rs.getString("family"))
                            .patronymic(rs.getString("patronymic"))
                            .birthDate(rs.getString("birthday"))
                            .build());
                }
            }, pinfl);

            // 4) Hech qanday qator topilmadi -> CLIENT_NOT_FOUND (HTTP 200, success=false)
            if (clientInfo.get() == null) {
                log.info("Client not found: requestId: {}", request.getRequestId());
                throw new ApiException(ErrorCode.CLIENT_NOT_FOUND);
            }

            return ApiResponse.ok(clientInfo.get());
        } catch (ApiException e) {
            // O'zimiz tashlagan tekshiruv xatolari: o'zgartirmasdan yuqoriga uzatiladi,
            // ularni controller'dagi @ExceptionHandler javobga aylantiradi
            throw e;
        } catch (Exception e) {
            // Kutilmagan xato (baza ulanishi, timeout, SQL xatosi va h.k.): logga yoziladi va ABS_* xato javobi qaytadi
            log.error(e.getMessage(), e);
            return checkRequest.absError(e);
        }
    }
}
