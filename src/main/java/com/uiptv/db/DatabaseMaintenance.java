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

    public static void requestVacuumAsync() {
        if (vacuumRunning.get()) {
            vacuumPending = true;
            return;
        }
        Thread thread = new Thread(() -> {
            vacuumRunning.set(true);
            try {
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

    private static void vacuum() {
        try (Connection conn = SQLConnection.connect(); Statement statement = conn.createStatement()) {
            statement.execute("VACUUM");
            AppLog.addInfoLog(DatabaseMaintenance.class, "Database vacuum completed.");
        } catch (SQLException e) {
            AppLog.addErrorLog(DatabaseMaintenance.class, "Database vacuum failed: " + e.getMessage());
        }
    }
}
