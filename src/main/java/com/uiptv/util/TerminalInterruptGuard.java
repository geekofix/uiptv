package com.uiptv.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class TerminalInterruptGuard {
    private static volatile boolean interruptRequested = false;
    private static final CountDownLatch currentStepDone = new CountDownLatch(1);

    private TerminalInterruptGuard() {
    }

    public static void install() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            interruptRequested = true;
            try {
                currentStepDone.await(30, TimeUnit.SECONDS);
                Thread.sleep(500);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        }, "uiptv-terminal-interrupt-guard"));
    }

    public static boolean isInterruptRequested() {
        return interruptRequested;
    }

    public static void finishStep() {
        currentStepDone.countDown();
    }
}
