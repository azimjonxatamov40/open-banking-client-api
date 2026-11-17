package com.is.openbankingclientapi.service;

import com.is.openbankingclientapi.component.CheckRequest;
import com.is.openbankingclientapi.model.ApiRequest;
import com.is.openbankingclientapi.model.account.AccountBaseInfo;
import com.is.openbankingclientapi.model.account.AccountDetailInfo;
import com.is.openbankingclientapi.model.account.AccountPayload;
import com.is.openbankingclientapi.model.account.BalancesPayload;
import com.is.openbankingclientapi.model.account.SearchPayload;
import com.is.openbankingclientapi.model.result.ApiException;
import com.is.openbankingclientapi.model.result.ApiResponse;
import com.is.openbankingclientapi.model.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

// Hisob raqamlari bilan ishlaydigan servis (4.1–4.4 metodlar). Ma'lumot ABS bazasidagi account jadvalidan olinadi.
// Barcha public metodlar bir xil qolipda yozilgan:
//   try { tekshirish -> bazadan o'qish -> ApiResponse.ok(...) }
//   catch (ApiException)  -> o'zgartirmasdan yuqoriga (controller'dagi @ExceptionHandler ushlaydi)
//   catch (Exception)     -> logga yozish va ABS_* xato javobini qaytarish
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final JdbcTemplate jdbcTemplate;
    private final CheckRequest checkRequest;

    // Hisobni o'qish uchun umumiy SELECT qismi; har bir metod oxiriga o'zining "where" shartini qo'shadi.
    // Bazadagi ichki kodlar SQL'ning o'zida (decode orqali) API qiymatlariga o'giriladi:
    //   sgn            — hisob turi: 'A' -> ACTIVE, 'P' -> PASSIVE (harflarning lotin va kirill varianti ham hisobga olingan)
    //   state          — holati: '2' -> OPEN, '3' -> CLOSED, null -> NOT_DETERMINED, qolgani OTHER
    //   acc_bal        — balans hisobining birinchi raqami hisob sinfini bildiradi: 1 aktivlar, 2 majburiyatlar,
    //                    3 kapital, 4 daromadlar, 5 xarajatlar, 9 ko'zda tutilmagan holatlar
    //   currency       — bazada so'm '000' deb saqlanadi, API'da ISO kodi '860' qaytariladi
    //   kernel.GetSOut — ABS funksiyasi: hisobning joriy operatsion kundagi (info.GetDay) chiquvchi qoldig'i
    private static final String ACCOUNT_SQL = "select a.id, \n" +
            "       a.branch, \n" +
            "       a.name, \n" +
            "       decode(a.sgn, 'A', 'ACTIVE', 'А', 'ACTIVE', 'P', 'PASSIVE', 'П', 'PASSIVE', 'OTHER') active_type, \n" +
            "       decode(a.state, '2', 'OPEN', '3', 'CLOSED', null, 'NOT_DETERMINED', 'OTHER') state, \n" +
            "       decode(substr(a.acc_bal, 1, 1), '1', 'ASSETS', '2', 'LIABILITIES', '3', 'EQUITY', \n" +
            "                                '4', 'INCOME', '5', 'EXPENSES', '9', 'CONTINGENT') account_class_type, \n" +
            "       decode(a.currency, '000', '860', a.currency) currency, \n" +
            "       kernel.GetSOut(a.id, info.GetDay, null, a.branch) saldo \n" +
            "  from account a \n";

    // 4.1 Bir nechta hisobning qoldig'ini qaytaradi
    public ApiResponse<List<AccountBaseInfo>> getBalances(ApiRequest<BalancesPayload> request) {
        try {
            checkRequest.checkHeader(request);

            // Hisoblar ro'yxati bo'sh bo'lmasligi kerak
            List<AccountPayload> bans = request.getPayload().getBans();
            if (bans == null || bans.isEmpty()) {
                throw new ApiException(ErrorCode.EMPTY_PARAM, "payload.bans");
            }

            // Har bir hisob alohida so'rov bilan o'qiladi va to'liq ma'lumotdan faqat qoldiqqa tegishli maydonlar olinadi.
            // Prefiks ("payload.bans[0]." kabi) xato chiqsa, aynan qaysi element noto'g'ri ekanini ko'rsatish uchun.
            // Bitta hisob ham topilmasa, getAccount ACC_NOT_FOUND tashlaydi va butun so'rov xato bilan tugaydi.
            List<AccountBaseInfo> balances = new ArrayList<>();
            for (int i = 0; i < bans.size(); i++) {
                AccountDetailInfo account = getAccount(request.getRequestId(), bans.get(i), "payload.bans[" + i + "].");
                balances.add(AccountBaseInfo.builder()
                        .requestId(account.getRequestId())
                        .ban(account.getBan())
                        .mfo(account.getMfo())
                        .currentBalance(account.getCurrentBalance())
                        .accountName(account.getAccountName())
                        .build());
            }

            return ApiResponse.ok(balances);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return checkRequest.absError(e);
        }
    }

    // 4.2 Mijozning hisoblarini filtrlar bo'yicha qidiradi
    public ApiResponse<List<AccountDetailInfo>> searchAccounts(ApiRequest<SearchPayload> request) {
        try {
            checkRequest.checkHeader(request);
            SearchPayload payload = request.getPayload();
            // Filial (mfo) va mijoz raqami har doim majburiy
            checkRequest.checkNotEmpty("payload.mfo", payload.getMfo(),
                    "payload.bankClientNumber", payload.getBankClientNumber());

            // Uchta ixtiyoriy filtrdan kamida bittasi berilgan bo'lishi kerak
            if (checkRequest.isEmpty(payload.getBalanceType())
                    && checkRequest.isEmpty(payload.getCcy())
                    && checkRequest.isEmpty(payload.getOrder())) {
                throw new ApiException(ErrorCode.EMPTY_PARAM, "payload.balanceType", "payload.ccy", "payload.order");
            }

            // SQL dinamik yig'iladi. args — "?" lar o'rniga qo'yiladigan qiymatlar, tartibi SQL'dagi "?" lar tartibi bilan bir xil.
            // filter(...) qiymat berilgan bo'lsa shartni SQL'ga, qiymatni args'ga qo'shadi; berilmagan bo'lsa hech narsa qo'shmaydi.
            List<Object> args = new ArrayList<>();
            args.add(payload.getMfo());
            args.add(payload.getBankClientNumber());

            String sql = ACCOUNT_SQL +
                    " where a.branch = ? \n" +
                    "   and a.client = ? \n" +
                    filter(args, "   and a.acc_bal = ? \n", payload.getBalanceType()) +
                    filter(args, "   and a.currency = ? \n", payload.getCcy()) +
                    filter(args, "   and a.id_order = ? \n", payload.getOrder()) ;

            // Har bir topilgan qator ro'yxatga qo'shiladi. Hech narsa topilmasa xato emas — bo'sh ro'yxat qaytadi.
            List<AccountDetailInfo> accounts = new ArrayList<>();
            jdbcTemplate.query(sql, (ResultSet rs) -> {
                accounts.add(createAccount(request.getRequestId(), rs));
            }, args.toArray());

            return ApiResponse.ok(accounts);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return checkRequest.absError(e);
        }
    }

    // 4.3 Bitta hisobning to'liq ma'lumotini qaytaradi (topilmasa ACC_NOT_FOUND)
    public ApiResponse<AccountDetailInfo> getDetails(ApiRequest<AccountPayload> request) {
        try {
            checkRequest.checkHeader(request);
            return ApiResponse.ok(getAccount(request.getRequestId(), request.getPayload(), "payload."));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return checkRequest.absError(e);
        }
    }

    // 4.4 Hisob mavjudligini tekshiradi. 4.3 dan farqi: topilmasa xato emas, data=false qaytadi.
    // Buning uchun ma'lumotni o'qimasdan, faqat qatorlar soni (count) sanaladi.
    public ApiResponse<Boolean> existsAccount(ApiRequest<AccountPayload> request) {
        try {
            checkRequest.checkHeader(request);
            AccountPayload payload = request.getPayload();
            checkRequest.checkNotEmpty("payload.ban", payload.getBan(), "payload.mfo", payload.getMfo());

            Integer count = jdbcTemplate.queryForObject("select count(*) from account a where a.id = ? and a.branch = ?",
                    Integer.class, payload.getBan(), payload.getMfo());

            return ApiResponse.ok(count > 0);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return checkRequest.absError(e);
        }
    }

    // 4.1 va 4.3 uchun umumiy metod: bitta hisobni raqami (ban) va filiali (mfo) bo'yicha o'qiydi.
    // prefix — xato javobidagi maydon nomining boshi ("payload." yoki "payload.bans[i].").
    // Hisob topilmasa ACC_NOT_FOUND tashlaydi, xato maydoni sifatida hisob raqamining o'zi beriladi.
    private AccountDetailInfo getAccount(String requestId, AccountPayload payload, String prefix) {
        checkRequest.checkNotEmpty(prefix + "ban", payload.getBan(), prefix + "mfo", payload.getMfo());

        // Natija lambda ichidan yozilishi uchun AtomicReference ishlatiladi; faqat birinchi qator olinadi
        AtomicReference<AccountDetailInfo> accountInfo = new AtomicReference<>();
        jdbcTemplate.query(ACCOUNT_SQL +
                " where a.id = ? \n" +
                "   and a.branch = ? ", (ResultSet rs) -> {
            if (accountInfo.get() == null) {
                accountInfo.set(createAccount(requestId, rs));
            }
        }, payload.getBan(), payload.getMfo());

        if (accountInfo.get() == null) {
            log.info("Account not found: requestId: {}, ban: {}", requestId, payload.getBan());
            throw new ApiException(ErrorCode.ACC_NOT_FOUND, payload.getBan());
        }

        return accountInfo.get();
    }

    // ResultSet'ning joriy qatoridan (ACCOUNT_SQL ustunlari) javob obyektini yig'adi.
    // saldo Long sifatida o'qiladi: qoldiq bo'lmasa 0 emas, null qaytadi.
    // accountType bazadan olinmaydi — hamma hisob uchun bir xil o'zgarmas qiymat.
    private AccountDetailInfo createAccount(String requestId, ResultSet rs) throws SQLException {
        return AccountDetailInfo.builder()
                .requestId(requestId)
                .ban(rs.getString("id"))
                .mfo(rs.getString("branch"))
                .currentBalance(rs.getObject("saldo", Long.class))
                .accountName(rs.getString("name"))
                .activeType(rs.getString("active_type"))
                .state(rs.getString("state"))
                .accountClassType(rs.getString("account_class_type"))
                .accountType("ACCOUNT_WITHOUT_OVERDRAFT")
                .currency(rs.getString("currency"))
                .build();
    }

    // Ixtiyoriy filtr uchun yordamchi: qiymat bo'sh bo'lsa "" qaytaradi (shart SQL'ga qo'shilmaydi),
    // aks holda qiymatni args'ga qo'shib, shart matnini qaytaradi. Shu tufayli "?" lar va args har doim mos tushadi.
    private String filter(List<Object> args, String condition, String value) {
        if (checkRequest.isEmpty(value)) {
            return "";
        }
        args.add(value);
        return condition;
    }
}
