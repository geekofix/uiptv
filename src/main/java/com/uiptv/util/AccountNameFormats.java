package com.uiptv.util;

import com.uiptv.db.AccountDb;
import com.uiptv.model.Account;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AccountNameFormats {
    private static final Pattern LEGACY_COMPACT_SUFFIX = Pattern.compile("^(.+)\\((\\d{1,3})\\)$");
    private static final Pattern SPACED_SUFFIX = Pattern.compile("^(.+) \\((\\d{1,4})\\)$");

    private AccountNameFormats() {
    }

    public static String formatAccountName(String baseName, int number) {
        return baseName + " (" + String.format("%04d", Math.max(number, 1)) + ")";
    }

    public static SuffixMatch matchSuffix(String accountName) {
        if (StringUtils.isBlank(accountName)) {
            return null;
        }
        Matcher spaced = SPACED_SUFFIX.matcher(accountName);
        if (spaced.matches()) {
            return new SuffixMatch(spaced.group(1).trim(), Integer.parseInt(spaced.group(2)), true);
        }
        Matcher compact = LEGACY_COMPACT_SUFFIX.matcher(accountName);
        if (compact.matches()) {
            return new SuffixMatch(compact.group(1).trim(), Integer.parseInt(compact.group(2)), false);
        }
        return null;
    }

    public static boolean isPlainName(String accountName) {
        return matchSuffix(accountName) == null;
    }

    public record SuffixMatch(String baseName, int number, boolean spaced) {
    }

    public static int normalizeExistingAccountNames() {
        return normalizeExistingAccountNames((processed, total, renamed) -> {
        });
    }

    public interface ProgressListener {
        void onProgress(int processed, int total, int renamed);
    }

    public static int normalizeExistingAccountNames(ProgressListener progressListener) {
        AccountDb accountDb = AccountDb.get();
        List<Account> accounts = accountDb.getAccounts();
        if (accounts == null || accounts.isEmpty()) {
            return 0;
        }
        int totalAccounts = accounts.size();
        int processedAccounts = 0;
        ProgressListener listener = progressListener != null ? progressListener : (processed, total, renamed) -> {
        };

        int renamed = 0;
        Map<String, List<Account>> groups = new LinkedHashMap<>();
        for (Account account : accounts) {
            String baseName = resolveBaseName(account.getAccountName());
            groups.computeIfAbsent(baseName, _ -> new ArrayList<>()).add(account);
        }

        for (Map.Entry<String, List<Account>> entry : groups.entrySet()) {
            String baseName = entry.getKey();
            List<Account> group = entry.getValue();
            boolean hasSuffixedMember = group.stream().anyMatch(a -> matchSuffix(a.getAccountName()) != null);

            if (!hasSuffixedMember) {
                continue;
            }

            Set<Integer> usedNumbers = new TreeSet<>();
            usedNumbers.add(0);
            for (Account account : group) {
                SuffixMatch match = matchSuffix(account.getAccountName());
                if (match != null) {
                    usedNumbers.add(match.number());
                }
            }

            int number = 0;
            for (Account account : group) {
                SuffixMatch match = matchSuffix(account.getAccountName());
                if (match != null) {
                    number = match.number();
                } else {
                    number = freeNumber(usedNumbers, number + 1);
                }
                String currentName = account.getAccountName();
                String newName = formatAccountName(baseName, number);
                usedNumbers.add(number);
                if (!newName.equals(currentName) && accountDb.getAccountByName(newName) == null) {
                    accountDb.updateAccountName(account, newName);
                    renamed++;
                }
                processedAccounts++;
                listener.onProgress(processedAccounts, totalAccounts, renamed);
            }
        }
        return renamed;
    }

    private static int freeNumber(Set<Integer> usedNumbers, int startNumber) {
        int candidate = Math.max(startNumber, 1);
        while (usedNumbers.contains(candidate)) {
            candidate++;
        }
        return candidate;
    }

    private static String resolveBaseName(String accountName) {
        SuffixMatch match = matchSuffix(accountName);
        return match != null ? match.baseName() : accountName;
    }
}
