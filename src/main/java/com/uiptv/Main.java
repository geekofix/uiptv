package com.uiptv;

import com.uiptv.service.AccountCleanupService;
import com.uiptv.ui.RootApplication;

public class Main {
    static void main(String[] args) {
        String firstArg = args != null && args.length > 0 ? args[0].trim() : "";
        if ("-names".equals(firstArg)) {
            long[] startedAt = {System.currentTimeMillis()};
            System.out.println("UIPTV: normalizing account names...");
            int renamed = com.uiptv.util.AccountNameFormats.normalizeExistingAccountNames((processed, total, count) -> {
                double percent = total <= 0 ? 100.0 : processed * 100.0 / total;
                long elapsed = System.currentTimeMillis() - startedAt[0];
                String eta = processed <= 0 ? "-" : (elapsed * (total - processed) / processed / 1000) + "s";
                System.out.printf("\rUIPTV: %d/%d accounts (%.0f%%) renamed: %d, remaining: %s   ",
                        processed, total, percent, count, eta);
                System.out.flush();
            });
            long durationSeconds = (System.currentTimeMillis() - startedAt[0]) / 1000;
            System.out.println();
            System.out.println("UIPTV: account names normalized, renamed accounts: " + renamed
                    + ", duration: " + durationSeconds + "s");
            System.exit(0);
        }
        if ("-expiry".equals(firstArg)) {
            long[] startedAt = {System.currentTimeMillis()};
            System.out.println("UIPTV: pinning accounts expiring within 7 days...");
            AccountCleanupService service = new AccountCleanupService();
            AccountCleanupService.ExpiryPinResult result = service.pinExpiringAccounts(new AccountCleanupService.CleanupListener() {
                private long lastPrintMillis = 0;

                @Override
                public void onProgress(int processed, int total) {
                    long now = System.currentTimeMillis();
                    if (now - lastPrintMillis < 100 && processed < total) {
                        return;
                    }
                    lastPrintMillis = now;
                    double percent = total <= 0 ? 100.0 : processed * 100.0 / total;
                    long elapsed = now - startedAt[0];
                    String eta = processed <= 0 ? "-" : (elapsed * (total - processed) / processed / 1000) + "s";
                    System.out.printf("\rUIPTV: %d/%d accounts (%.0f%%) remaining: %s   ",
                            processed, total, percent, eta);
                    System.out.flush();
                }

                @Override
                public void onLog(String message) {
                    System.out.println();
                    System.out.println("UIPTV: expiry pin changed: " + message);
                }
            });
            long durationSeconds = (System.currentTimeMillis() - startedAt[0]) / 1000;
            System.out.println();
            System.out.println("UIPTV: expiry pins set: " + result.pinned()
                    + ", removed: " + result.unpinned() + ", duration: " + durationSeconds + "s");
            System.exit(0);
        }
        if ("-duplicates".equals(firstArg)) {
            long[] startedAt = {System.currentTimeMillis()};
            System.out.println("UIPTV: removing duplicate accounts...");
            int[] removed = {0};
            AccountCleanupService service = new AccountCleanupService();
            AccountCleanupService.CleanupListener listener = new AccountCleanupService.CleanupListener() {
                private int lastProcessed = 0;
                private long lastPrintMillis = 0;

                @Override
                public void onProgress(int processed, int total) {
                    long now = System.currentTimeMillis();
                    if (now - lastPrintMillis < 100 && processed < total) {
                        return;
                    }
                    lastPrintMillis = now;
                    lastProcessed = processed;
                    double percent = total <= 0 ? 100.0 : processed * 100.0 / total;
                    long elapsed = now - startedAt[0];
                    String eta = processed <= 0 ? "-" : (elapsed * (total - processed) / processed / 1000) + "s";
                    System.out.printf("\rUIPTV: %d/%d accounts (%.0f%%) duplicates removed: %d, remaining: %s   ",
                            processed, total, percent, removed[0], eta);
                    System.out.flush();
                }

                @Override
                public void onLog(String message) {
                    System.out.println();
                    System.out.println("UIPTV: removed duplicate: " + message);
                    removed[0]++;
                }
            };
            int removedCount = service.removeDuplicatesOnly(listener);
            long durationSeconds = (System.currentTimeMillis() - startedAt[0]) / 1000;
            System.out.println();
            System.out.println("UIPTV: duplicates removed: " + removedCount
                    + ", duration: " + durationSeconds + "s");
            System.exit(0);
        }
        RootApplication.main(args);
    }
}
