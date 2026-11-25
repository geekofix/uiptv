package com.uiptv;

import com.uiptv.ui.RootApplication;

public class Main {
    static void main(String[] args) {
        if (args != null && args.length > 0 && "-names".equals(args[0].trim())) {
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
        RootApplication.main(args);
    }
}
