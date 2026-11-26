package com.uiptv;

import com.uiptv.service.AccountCleanupService;
import com.uiptv.ui.RootApplication;
import com.uiptv.util.TerminalInterruptGuard;

public class Main {
    static void main(String[] args) {
        String firstArg = args != null && args.length > 0 ? args[0].trim() : "";
        if ("-names".equals(firstArg)) {
            TerminalInterruptGuard.install();
            long[] startedAt = {System.currentTimeMillis()};
            System.out.println("UIPTV: normalizing account names...");
            int renamed = 0;
            boolean interrupted = false;
            try {
                renamed = com.uiptv.util.AccountNameFormats.normalizeExistingAccountNames((processed, total, count) -> {
                    TerminalInterruptGuard.finishStep();
                    if (TerminalInterruptGuard.isInterruptRequested()) {
                        throw new IllegalStateException("interrupted");
                    }
                    double percent = total <= 0 ? 100.0 : processed * 100.0 / total;
                    long elapsed = System.currentTimeMillis() - startedAt[0];
                    String eta = processed <= 0 ? "-" : (elapsed * (total - processed) / processed / 1000) + "s";
                    System.out.printf("\rUIPTV: %d/%d accounts (%.0f%%) renamed: %d, remaining: %s   ",
                            processed, total, percent, count, eta);
                    System.out.flush();
                });
            } catch (IllegalStateException _) {
                interrupted = true;
            }
            long durationSeconds = (System.currentTimeMillis() - startedAt[0]) / 1000;
            System.out.println();
            if (interrupted) {
                System.out.println("UIPTV: interrupted, exiting after the current account.");
            }
            System.out.println("UIPTV: account names normalized, renamed accounts: " + renamed
                    + ", duration: " + durationSeconds + "s");
            System.out.flush();
            TerminalInterruptGuard.finishStep();
            System.exit(0);
        }
        if ("-duplicates".equals(firstArg)) {
            TerminalInterruptGuard.install();
            long[] startedAt = {System.currentTimeMillis()};
            System.out.println("UIPTV: removing duplicate accounts...");
            AccountCleanupService service = new AccountCleanupService();
            AccountCleanupService.CleanupListener listener = new AccountCleanupService.CleanupListener() {
                private long lastPrintMillis = 0;

                @Override
                public void onProgress(int processed, int total) {
                    TerminalInterruptGuard.finishStep();
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
                    System.out.println("UIPTV: removed duplicate: " + message);
                }

                @Override
                public boolean isStopRequested() {
                    return TerminalInterruptGuard.isInterruptRequested();
                }
            };
            int removedCount = service.removeDuplicatesOnly(listener);
            long durationSeconds = (System.currentTimeMillis() - startedAt[0]) / 1000;
            System.out.println();
            if (TerminalInterruptGuard.isInterruptRequested()) {
                System.out.println("UIPTV: interrupted, exiting after the current account.");
            }
            System.out.println("UIPTV: duplicates removed: " + removedCount
                    + ", duration: " + durationSeconds + "s");
            System.out.flush();
            TerminalInterruptGuard.finishStep();
            System.exit(0);
        }
        if ("-expiry".equals(firstArg)) {
            TerminalInterruptGuard.install();
            long[] startedAt = {System.currentTimeMillis()};
            System.out.println("UIPTV: pinning accounts expiring within 7 days...");
            AccountCleanupService service = new AccountCleanupService();
            AccountCleanupService.ExpiryPinResult result = service.pinExpiringAccounts(new AccountCleanupService.CleanupListener() {
                private long lastPrintMillis = 0;

                @Override
                public void onProgress(int processed, int total) {
                    TerminalInterruptGuard.finishStep();
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

                @Override
                public boolean isStopRequested() {
                    return TerminalInterruptGuard.isInterruptRequested();
                }
            });
            long durationSeconds = (System.currentTimeMillis() - startedAt[0]) / 1000;
            System.out.println();
            if (TerminalInterruptGuard.isInterruptRequested()) {
                System.out.println("UIPTV: interrupted, exiting after the current account.");
            }
            System.out.println("UIPTV: expiry pins set: " + result.pinned()
                    + ", removed: " + result.unpinned() + ", duration: " + durationSeconds + "s");
            System.out.flush();
            TerminalInterruptGuard.finishStep();
            System.exit(0);
        }
        RootApplication.main(args);
    }
}
