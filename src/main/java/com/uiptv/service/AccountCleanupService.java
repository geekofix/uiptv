package com.uiptv.service;

import com.uiptv.db.DatabaseMaintenance;
import com.uiptv.model.Account;
import com.uiptv.util.AccountNameFormats;
import com.uiptv.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.uiptv.util.AccountType.STALKER_PORTAL;
import static com.uiptv.util.AccountType.XTREME_API;

public class AccountCleanupService {

    public interface CleanupListener {
        void onProgress(int processed, int total);

        void onLog(String message);

        default boolean isStopRequested() {
            return false;
        }
    }

    public record CleanupReport(int renamedAccounts, int removedDuplicates, boolean stoppedEarly) {
    }

    public CleanupReport run(CleanupListener listener) {
        CleanupListener activeListener = wrapListener(listener);
        int renamed = normalizeNames(activeListener);
        int removedDuplicates = removeDuplicates(activeListener);
        DatabaseMaintenance.requestVacuumAsync();
        return new CleanupReport(renamed, removedDuplicates, activeListener.isStopRequested());
    }

    public int removeDuplicatesOnly(CleanupListener listener) {
        CleanupListener activeListener = wrapListener(listener);
        int removedDuplicates = removeDuplicates(activeListener);
        DatabaseMaintenance.requestVacuumAsync();
        return removedDuplicates;
    }

    private CleanupListener wrapListener(CleanupListener listener) {
        return listener != null ? listener : new CleanupListener() {
            @Override
            public void onProgress(int processed, int total) {
            }

            @Override
            public void onLog(String message) {
            }
        };
    }

    private int normalizeNames(CleanupListener listener) {
        int[] renamedHolder = {0};
        int renamed = AccountNameFormats.normalizeExistingAccountNames((processed, total, renamedCount) -> {
            renamedHolder[0] = renamedCount;
            listener.onProgress(processed, total);
        });
        return renamed > 0 ? renamed : renamedHolder[0];
    }

    private int removeDuplicates(CleanupListener listener) {
        Map<String, Account> accounts = AccountService.getInstance().getAll();
        if (accounts.isEmpty()) {
            return 0;
        }
        List<Account> orderedAccounts = new ArrayList<>(accounts.values());
        int total = orderedAccounts.size();
        Map<String, Account> seenByDedupeKey = new LinkedHashMap<>();
        int removed = 0;
        int processed = 0;
        for (Account account : orderedAccounts) {
            if (listener.isStopRequested()) {
                return removed;
            }
            processed++;
            listener.onProgress(processed, total);
            String dedupeKey = dedupeKey(account);
            if (dedupeKey == null) {
                continue;
            }
            Account kept = seenByDedupeKey.get(dedupeKey);
            if (kept == null) {
                seenByDedupeKey.put(dedupeKey, account);
                continue;
            }
            AccountService.getInstance().delete(account.getDbId());
            removed++;
            listener.onLog(account.getAccountName() + " -> " + kept.getAccountName());
        }
        return removed;
    }

    private String dedupeKey(Account account) {
        if (account == null || StringUtils.isBlank(account.getDbId())) {
            return null;
        }
        if (account.getType() == STALKER_PORTAL) {
            if (StringUtils.isBlank(account.getUrl()) || StringUtils.isBlank(account.getMacAddress())) {
                return null;
            }
            return "stalker|" + normalizeServer(account.getUrl())
                    + "|" + account.getMacAddress().trim().toLowerCase(java.util.Locale.ROOT);
        }
        if (account.getType() == XTREME_API) {
            if (StringUtils.isBlank(account.getUrl()) || StringUtils.isBlank(account.getUsername())) {
                return null;
            }
            return "xtream|" + normalizeServer(account.getUrl())
                    + "|" + account.getUsername().trim()
                    + "|" + (account.getPassword() == null ? "" : account.getPassword().trim());
        }
        if (StringUtils.isNotBlank(account.getM3u8Path())) {
            return account.getType() + "|" + account.getM3u8Path().trim().toLowerCase(java.util.Locale.ROOT);
        }
        return null;
    }

    private String normalizeServer(String url) {
        String normalized = url == null ? "" : url.trim().toLowerCase(java.util.Locale.ROOT);
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
