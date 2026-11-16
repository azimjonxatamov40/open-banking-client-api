package com.is.openbankingclientapi.controller;

import com.is.openbankingclientapi.model.ApiRequest;
import com.is.openbankingclientapi.model.account.AccountPayload;
import com.is.openbankingclientapi.model.account.BalancesPayload;
import com.is.openbankingclientapi.model.account.SearchPayload;
import com.is.openbankingclientapi.model.client.ClientPayload;
import com.is.openbankingclientapi.model.result.ApiException;
import com.is.openbankingclientapi.model.result.ApiResponse;
import com.is.openbankingclientapi.service.AccountService;
import com.is.openbankingclientapi.service.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OpenBankingController {

    private final ClientService clientService;
    private final AccountService accountService;

    // 3.1 Mijoz haqida ma'lumot
    @PostMapping("/clients/info")
    public ResponseEntity<Object> clientInfo(@RequestBody ApiRequest<ClientPayload> request) {
        log.info("ClientInfo start: requestId: {}", request.getRequestId());
        return send(clientService.getClientInfo(request));
    }

    // 4.1 Hisoblar qoldig'i
    @PostMapping("/accounts/balances")
    public ResponseEntity<Object> balances(@RequestBody ApiRequest<BalancesPayload> request) {
        log.info("Balances start: requestId: {}", request.getRequestId());
        return send(accountService.getBalances(request));
    }

    // 4.2 Hisoblarni qidirish
    @PostMapping("/accounts/search")
    public ResponseEntity<Object> search(@RequestBody ApiRequest<SearchPayload> request) {
        log.info("Search start: requestId: {}", request.getRequestId());
        return send(accountService.searchAccounts(request));
    }

    // 4.3 Hisob haqida batafsil ma'lumot
    @PostMapping("/accounts/details")
    public ResponseEntity<Object> details(@RequestBody ApiRequest<AccountPayload> request) {
        log.info("Details start: requestId: {}", request.getRequestId());
        return send(accountService.getDetails(request));
    }

    // 4.4 Hisob mavjudligini tekshirish
    @PostMapping("/accounts/exists")
    public ResponseEntity<Object> exists(@RequestBody ApiRequest<AccountPayload> request) {
        log.info("Exists start: requestId: {}", request.getRequestId());
        return send(accountService.existsAccount(request));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> requestError(ApiException e) {
        return send(ApiResponse.error(e.getCode(), e.getFields()));
    }

    private ResponseEntity<Object> send(ApiResponse<?> res) {
        if (res.getHttpStatus() == 200) {
            return ResponseEntity.ok(res);
        }
        return ResponseEntity.status(res.getHttpStatus()).body(res.getError());
    }
}
