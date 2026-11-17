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

// Open Banking API'ning HTTP qatlami: tashqi tizimdan kelgan so'rovlarni qabul qilib, tegishli servisga uzatadi.
// Bu yerda biznes logika yo'q — faqat so'rovni log qilish, servisni chaqirish va javobni HTTP ko'rinishiga o'tkazish.
//
// @Slf4j                   — Lombok: "log" nomli logger maydonini yaratadi
// @RestController          — metodlar qaytargan obyekt avtomatik JSON'ga aylantiriladi
// @RequestMapping          — barcha endpointlar /api/v1 bilan boshlanadi
// @RequiredArgsConstructor — Lombok: final maydonlar uchun konstruktor yaratadi, Spring servislarni shu orqali beradi
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OpenBankingController {

    private final ClientService clientService;
    private final AccountService accountService;

    // Barcha endpointlar POST va bir xil tuzilishdagi tanani kutadi:
    // { "requestId": "...", "dsign": "...", "payload": { ... } }
    // @RequestBody JSON'ni ApiRequest<T> ga o'giradi, T — har bir endpointning o'z payload turi.

    // 3.1 Mijoz haqida ma'lumot
    // Kirish: payload.pinfl (14 xonali JSHSHIR). Chiqish: mijozning shaxsiy ma'lumotlari.
    @PostMapping("/clients/info")
    public ResponseEntity<Object> clientInfo(@RequestBody ApiRequest<ClientPayload> request) {
        log.info("ClientInfo start: requestId: {}", request.getRequestId());
        return send(clientService.getClientInfo(request));
    }

    // 4.1 Hisoblar qoldig'i
    // Kirish: payload.bans — {ban, mfo} juftliklari ro'yxati. Chiqish: har bir hisobning qoldig'i va nomi.
    @PostMapping("/accounts/balances")
    public ResponseEntity<Object> balances(@RequestBody ApiRequest<BalancesPayload> request) {
        log.info("Balances start: requestId: {}", request.getRequestId());
        return send(accountService.getBalances(request));
    }

    // 4.2 Hisoblarni qidirish
    // Kirish: payload.mfo + payload.bankClientNumber (majburiy) va balanceType / ccy / order filtrlaridan kamida bittasi.
    // Chiqish: mijozning shartga mos hisoblari ro'yxati.
    @PostMapping("/accounts/search")
    public ResponseEntity<Object> search(@RequestBody ApiRequest<SearchPayload> request) {
        log.info("Search start: requestId: {}", request.getRequestId());
        return send(accountService.searchAccounts(request));
    }

    // 4.3 Hisob haqida batafsil ma'lumot
    // Kirish: payload.ban + payload.mfo. Chiqish: bitta hisobning to'liq ma'lumoti.
    @PostMapping("/accounts/details")
    public ResponseEntity<Object> details(@RequestBody ApiRequest<AccountPayload> request) {
        log.info("Details start: requestId: {}", request.getRequestId());
        return send(accountService.getDetails(request));
    }

    // 4.4 Hisob mavjudligini tekshirish
    // Kirish: payload.ban + payload.mfo. Chiqish: data = true/false.
    @PostMapping("/accounts/exists")
    public ResponseEntity<Object> exists(@RequestBody ApiRequest<AccountPayload> request) {
        log.info("Exists start: requestId: {}", request.getRequestId());
        return send(accountService.existsAccount(request));
    }

    // Servislar tekshiruvdan o'tmagan so'rov yoki topilmagan ma'lumot uchun ApiException tashlaydi.
    // Shu controller ichida tashlangan har qanday ApiException shu yerga tushadi va
    // xato kodi + muammoli maydonlar ro'yxati bilan standart xato javobiga aylantiriladi.
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> requestError(ApiException e) {
        return send(ApiResponse.error(e.getCode(), e.getFields()));
    }

    // Javobni HTTP ko'rinishiga o'tkazadi. Ikki xil format bor:
    //  - httpStatus 200: butun konvert qaytadi {data, success, error}.
    //    Bunga "topilmadi" xatolari ham kiradi (ACC_NOT_FOUND, CLIENT_NOT_FOUND) — ular success=false bilan 200 qaytaradi.
    //  - boshqa status (400, 500, 502, 504): faqat error obyekti qaytadi {code, message, cause, fields}.
    private ResponseEntity<Object> send(ApiResponse<?> res) {
        if (res.getHttpStatus() == 200) {
            return ResponseEntity.ok(res);
        }
        return ResponseEntity.status(res.getHttpStatus()).body(res.getError());
    }
}
