package com.uiptv.db;

import com.uiptv.util.AppLog;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

public class DatabaseMaintenance {
    private static final AtomicBoolean vacuumRunning = new AtomicBoolean(false);
    private static volatile boolean vacuumPending = false;

    private DatabaseMaintenance() {
    }

    private static final long VACUUM_QUIET_PERIOD_MS = 8000;

    public static void requestVacuumAsync() {
        if (vacuumRunning.get()) {
            vacuumPending = true;
            return;
        }
        Thread thread = new Thread(() -> {
            vacuumRunning.set(true);
            try {
                sleepQuietly(VACUUM_QUIET_PERIOD_MS);
                do {
                    vacuumPending = false;
                    vacuum();
                } while (vacuumPending);
            } finally {
                vacuumRunning.set(false);
            }
        }, "uiptv-db-vacuum");
        thread.setDaemon(true);
        thread.start();
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void vacuum() {
        try (Connection conn = SQLConnection.connect(); Statement statement = conn.createStatement()) {
            statement.execute("VACUUM");
            try {
                statement.execute("PRAGMA wal_checkpoint(TRUNCATE)");
            } catch (SQLException _) {
                // Checkpoint is best-effort; a busy WAL just keeps its size until the next run.
            }
            AppLog.addInfoLog(DatabaseMaintenance.class, "Database vacuum completed.");
        } catch (SQLException e) {
            AppLog.addErrorLog(DatabaseMaintenance.class, "Database vacuum failed: " + e.getMessage());
        }
    }
}
