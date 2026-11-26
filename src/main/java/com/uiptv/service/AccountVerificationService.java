package com.uiptv.service;

import com.uiptv.model.Account;
import com.uiptv.model.AccountInfo;
import com.uiptv.model.AccountStatus;
import com.uiptv.ui.AccountInfoUiUtil;
import com.uiptv.ui.XtremeParser;
import com.uiptv.util.AccountType;
import com.uiptv.util.FetchAPI;
import com.uiptv.util.HttpUtil;
import com.uiptv.util.StringUtils;
import com.uiptv.util.XtremeCredentialsJson;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.uiptv.model.Account.AccountAction.itv;

public class AccountVerificationService {
    private static final int VERIFY_TIMEOUT_SECONDS = 15;
    private static final HttpUtil.RequestOptions VERIFY_REQUEST_OPTIONS =
            new HttpUtil.RequestOptions(true, true, VERIFY_TIMEOUT_SECONDS, VERIFY_TIMEOUT_SECONDS, VERIFY_TIMEOUT_SECONDS);


    public record CredentialCheck(String label, boolean valid, boolean serverError, String errorMessage,
                                   String expireDate, String accountStatus) {
    }

    public boolean supports(Account account) {
        return account != null && (account.getType() == AccountType.STALKER_PORTAL || account.getType() == AccountType.XTREME_API);
    }

    public List<CredentialCheck> verifyAccount(Account account) {
        if (account.getType() == AccountType.STALKER_PORTAL) {
            return verifyStalkerAccount(account);
        }
        if (account.getType() == AccountType.XTREME_API) {
            return verifyXtremeAccount(account);
        }
        return List.of();
    }

    public int checkCount(Account account) {
        if (account.getType() == AccountType.STALKER_PORTAL) {
            return stalkerMacs(account).size();
        }
        if (account.getType() == AccountType.XTREME_API) {
            return xtremeCredentials(account).size();
        }
        return 0;
    }

    private List<CredentialCheck> verifyStalkerAccount(Account account) {
        AccountInfo info = AccountInfoService.getInstance().getByAccountId(account.getDbId());
        String expireDate = formatExpireDate(info != null ? info.getExpireDate() : null);
        String status = info != null && info.getAccountStatus() != null ? info.getAccountStatus().toDisplay() : "unknown";
        List<CredentialCheck> checks = new ArrayList<>();
        for (String mac : stalkerMacs(account)) {
            checks.add(verifyStalkerMac(account, mac, expireDate, status));
        }
        return checks;
    }

    private CredentialCheck verifyStalkerMac(Account account, String mac, String expireDate, String status) {
        String originalMac = account.getMacAddress();
        try {
            account.setMacAddress(mac);
            HandshakeService.getInstance().connect(account);
            if (account.isNotConnected()) {
                return new CredentialCheck(mac, false, false, null, expireDate, status);
            }
            String jsonCategories = FetchAPI.fetch(stalkerCategoryParams(account.getAction()), account, VERIFY_REQUEST_OPTIONS);
            boolean valid = !CategoryService.getInstance().parseCategories(jsonCategories, false).isEmpty();
            return new CredentialCheck(mac, valid, false, null, expireDate, status);
        } catch (Exception e) {
            return new CredentialCheck(mac, false, true, rootMessage(e), expireDate, status);
        } finally {
            account.setMacAddress(originalMac);
        }
    }

    private List<CredentialCheck> verifyXtremeAccount(Account account) {
        List<CredentialCheck> checks = new ArrayList<>();
        for (XtremeCredentialsJson.Entry entry : xtremeCredentials(account)) {
            String label = entry.username() + ":" + entry.password();
            String originalUsername = account.getUsername();
            String originalPassword = account.getPassword();
            try {
                account.setUsername(entry.username());
                account.setPassword(entry.password());
                JSONObject userInfo = XtremeParser.fetchUserInfo(account, VERIFY_REQUEST_OPTIONS);
                boolean valid = userInfo != null && !"0".equals(userInfo.optString("auth"));
                String expireDate = formatExpireDate(userInfo == null ? null : userInfo.optString("exp_date", ""));
                String status = resolveXtreamStatus(userInfo);
                returnXtremeCredentials(account, originalUsername, originalPassword);
                checks.add(new CredentialCheck(label, valid, false, null, expireDate, status));
            } catch (Exception e) {
                returnXtremeCredentials(account, originalUsername, originalPassword);
                String message = rootMessage(e);
                if (message.contains("HTTP 401") || message.contains("HTTP 403")) {
                    checks.add(new CredentialCheck(label, false, false, null, "Unknown", "unknown"));
                } else {
                    checks.add(new CredentialCheck(label, false, true, message, "Unknown", "unknown"));
                }
            }
        }
        return checks;
    }

    private String resolveXtreamStatus(JSONObject userInfo) {
        String rawStatus = userInfo == null ? "" : userInfo.optString("status", "");
        if (StringUtils.isBlank(rawStatus) || "null".equals(rawStatus)) {
            return "unknown";
        }
        AccountStatus parsedStatus = AccountStatus.fromValue(rawStatus);
        return parsedStatus != null ? parsedStatus.toDisplay() : rawStatus;
    }

    private void returnXtremeCredentials(Account account, String username, String password) {
        account.setUsername(username);
        account.setPassword(password);
    }

    private List<String> stalkerMacs(Account account) {
        Set<String> macs = new LinkedHashSet<>();
        if (StringUtils.isNotBlank(account.getMacAddress())) {
            macs.add(account.getMacAddress().trim());
        }
        if (StringUtils.isNotBlank(account.getMacAddressList())) {
            Arrays.stream(account.getMacAddressList().split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .forEach(macs::add);
        }
        return new ArrayList<>(macs);
    }

    private List<XtremeCredentialsJson.Entry> xtremeCredentials(Account account) {
        List<XtremeCredentialsJson.Entry> entries = XtremeCredentialsJson.parse(account.getXtremeCredentialsJson());
        if (entries.isEmpty() && StringUtils.isNotBlank(account.getUsername()) && StringUtils.isNotBlank(account.getPassword())) {
            entries.add(new XtremeCredentialsJson.Entry(account.getUsername(), account.getPassword(), true));
        }
        return entries;
    }

    private String formatExpireDate(String rawExpireDate) {
        String formatted = AccountInfoUiUtil.formatDate(rawExpireDate);
        return StringUtils.isBlank(formatted) ? "Unknown" : formatted;
    }

    private String rootMessage(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return StringUtils.isNotBlank(message) ? message : cause.getClass().getSimpleName();
    }

    private Map<String, String> stalkerCategoryParams(Account.AccountAction accountAction) {
        final Map<String, String> params = new HashMap<>();
        params.put("JsHttpRequest", new Date().getTime() + "-xml");
        params.put("type", accountAction.name());
        params.put("action", accountAction == itv ? "get_genres" : "get_categories");
        return params;
    }
}
