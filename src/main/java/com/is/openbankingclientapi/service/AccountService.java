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

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final JdbcTemplate jdbcTemplate;
    private final CheckRequest checkRequest;

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

    public ApiResponse<List<AccountBaseInfo>> getBalances(ApiRequest<BalancesPayload> request) {
        try {
            checkRequest.checkHeader(request);

            List<AccountPayload> bans = request.getPayload().getBans();
            if (bans == null || bans.isEmpty()) {
                throw new ApiException(ErrorCode.EMPTY_PARAM, "payload.bans");
            }

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

    public ApiResponse<List<AccountDetailInfo>> searchAccounts(ApiRequest<SearchPayload> request) {
        try {
            checkRequest.checkHeader(request);
            SearchPayload payload = request.getPayload();
            checkRequest.checkNotEmpty("payload.mfo", payload.getMfo(),
                    "payload.bankClientNumber", payload.getBankClientNumber());

            if (checkRequest.isEmpty(payload.getBalanceType())
                    && checkRequest.isEmpty(payload.getCcy())
                    && checkRequest.isEmpty(payload.getOrder())) {
                throw new ApiException(ErrorCode.EMPTY_PARAM, "payload.balanceType", "payload.ccy", "payload.order");
            }

            List<Object> args = new ArrayList<>();
            args.add(payload.getMfo());
            args.add(payload.getBankClientNumber());

            String sql = ACCOUNT_SQL +
                    " where a.branch = ? \n" +
                    "   and a.client = ? \n" +
                    filter(args, "   and a.acc_bal = ? \n", payload.getBalanceType()) +
                    filter(args, "   and a.currency = ? \n", payload.getCcy()) +
                    filter(args, "   and a.id_order = ? \n", payload.getOrder()) +
                    " order by a.id";

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

    private AccountDetailInfo getAccount(String requestId, AccountPayload payload, String prefix) {
        checkRequest.checkNotEmpty(prefix + "ban", payload.getBan(), prefix + "mfo", payload.getMfo());

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

    private String filter(List<Object> args, String condition, String value) {
        if (checkRequest.isEmpty(value)) {
            return "";
        }
        args.add(value);
        return condition;
    }
}
