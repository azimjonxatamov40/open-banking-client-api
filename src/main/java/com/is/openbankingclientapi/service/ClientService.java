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

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private final JdbcTemplate jdbcTemplate;
    private final CheckRequest checkRequest;

    public ApiResponse<ClientInfo> getClientInfo(ApiRequest<ClientPayload> request) {
        try {
            checkRequest.checkHeader(request);
            String pinfl = request.getPayload().getPinfl();
            checkRequest.checkNotEmpty("payload.pinfl", pinfl);

            if (!pinfl.matches("\\d{14}")) {
                throw new ApiException(ErrorCode.INVALID_PARAM, "payload.pinfl");
            }

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

            if (clientInfo.get() == null) {
                log.info("Client not found: requestId: {}", request.getRequestId());
                throw new ApiException(ErrorCode.CLIENT_NOT_FOUND);
            }

            return ApiResponse.ok(clientInfo.get());
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return checkRequest.absError(e);
        }
    }
}
