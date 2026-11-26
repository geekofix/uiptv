package com.uiptv.service;

import com.uiptv.model.Account;
import com.uiptv.model.AccountInfo;
import com.uiptv.model.AccountStatus;
import com.uiptv.ui.AccountInfoUiUtil;
import com.uiptv.ui.XtremeParser;
import com.uiptv.util.AccountType;
import com.uiptv.util.StringUtils;
import com.uiptv.util.XtremeCredentialsJson;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AccountVerificationService {

    public record CredentialCheck(String label, boolean valid, String expireDate, String accountStatus) {
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

    private List<CredentialCheck> verifyStalkerAccount(Account account) {
        CacheService cacheService = new CacheServiceImpl();
        AccountInfo info = AccountInfoService.getInstance().getByAccountId(account.getDbId());
        String expireDate = formatExpireDate(info != null ? info.getExpireDate() : null);
        String status = info != null && info.getAccountStatus() != null ? info.getAccountStatus().toDisplay() : "unknown";
        List<CredentialCheck> checks = new ArrayList<>();
        for (String mac : stalkerMacs(account)) {
            boolean valid = cacheService.verifyMacAddress(account, mac);
            checks.add(new CredentialCheck(mac, valid, expireDate, status));
        }
        return checks;
    }

    private List<CredentialCheck> verifyXtremeAccount(Account account) {
        List<CredentialCheck> checks = new ArrayList<>();
        for (XtremeCredentialsJson.Entry entry : xtremeCredentials(account)) {
            String label = entry.username() + ":" + entry.password();
            String expireDate;
            String status;
            boolean valid;
            String originalUsername = account.getUsername();
            String originalPassword = account.getPassword();
            try {
                account.setUsername(entry.username());
                account.setPassword(entry.password());
                JSONObject userInfo = XtremeParser.fetchUserInfo(account);
                valid = userInfo != null && !"0".equals(userInfo.optString("auth"));
                expireDate = formatExpireDate(userInfo == null ? null : userInfo.optString("exp_date", ""));
                String rawStatus = userInfo == null ? "" : userInfo.optString("status", "");
                if (StringUtils.isBlank(rawStatus) || "null".equals(rawStatus)) {
                    status = "unknown";
                } else {
                    AccountStatus parsedStatus = AccountStatus.fromValue(rawStatus);
                    status = parsedStatus != null ? parsedStatus.toDisplay() : rawStatus;
                }
            } catch (Exception e) {
                valid = false;
                expireDate = "Unknown";
                status = "unknown";
            } finally {
                account.setUsername(originalUsername);
                account.setPassword(originalPassword);
            }
            checks.add(new CredentialCheck(label, valid, expireDate, status));
        }
        return checks;
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
}
