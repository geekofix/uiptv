package com.uiptv.ui;

import com.uiptv.service.AccountCleanupService;
import com.uiptv.util.I18n;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CleanAccountsPopup extends VBox {

    private final Stage stage;
    private final boolean normalizeNames;
    private final TextArea logArea = new TextArea();
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label remainingLabel = new Label();
    private final Button stopButton = new Button(I18n.tr("cleanAccountsStop"));
    private final Button copyLogButton = new Button(I18n.tr("autoCopyLog"));
    private final Button exportLogButton = new Button(I18n.tr("reloadExportLog"));
    private final Button closeButton = new Button(I18n.tr("autoClose"));
    private final StringBuilder logBuilder = new StringBuilder();
    private volatile boolean stopRequested = false;

    private CleanAccountsPopup(Stage stage, boolean normalizeNames) {
        this.stage = stage;
        this.normalizeNames = normalizeNames;
        setSpacing(10);
        setPadding(new Insets(12));
        buildContent();
        startCleanupInBackground();
    }

    public static void showPopup(Stage owner) {
        showPopup(owner, true);
    }

    public static void showPopup(Stage owner, boolean normalizeNames) {
        Stage popupStage = new Stage();
        if (owner != null) {
            popupStage.initOwner(owner);
            popupStage.initModality(Modality.WINDOW_MODAL);
        }
        CleanAccountsPopup popup = new CleanAccountsPopup(popupStage, normalizeNames);
        Scene scene = new Scene(popup, 720, 520);
        I18n.applySceneOrientation(scene);
        if (RootApplication.getCurrentTheme() != null) {
            scene.getStylesheets().add(RootApplication.getCurrentTheme());
        }
        popupStage.setTitle(I18n.tr(normalizeNames ? "cleanAccountsTitle" : "clearDuplicatesTitle"));
        popupStage.setScene(scene);
        popupStage.showAndWait();
    }

    private void buildContent() {
        logArea.setEditable(false);
        logArea.setWrapText(true);
        VBox.setVgrow(logArea, Priority.ALWAYS);
        logArea.setMaxHeight(Double.MAX_VALUE);

        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(18);

        remainingLabel.getStyleClass().add("dim-label");

        stopButton.setOnAction(event -> stopRequested = true);
        copyLogButton.setOnAction(event -> copyLogToClipboard());
        exportLogButton.setOnAction(event -> exportLogToFile());
        closeButton.setOnAction(event -> stage.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttonBox = new HBox(10, stopButton, spacer, copyLogButton, exportLogButton, closeButton);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        getChildren().setAll(progressBar, remainingLabel, logArea, buttonBox);
    }

    private void startCleanupInBackground() {
        Thread thread = new Thread(this::runCleanup, "uiptv-clean-accounts");
        thread.setDaemon(true);
        thread.start();
    }

    private void runCleanup() {
        long startMillis = System.currentTimeMillis();
        AccountCleanupService service = new AccountCleanupService();
        AccountCleanupService.CleanupListener listener = new AccountCleanupService.CleanupListener() {
            private int lastProcessed = 0;
            private int lastTotal = 0;
            private long lastUpdateMillis = 0;

            @Override
            public void onProgress(int processed, int total) {
                if (total <= 0) {
                    return;
                }
                lastProcessed = processed;
                lastTotal = total;
                long now = System.currentTimeMillis();
                if (now - lastUpdateMillis < 100) {
                    return;
                }
                lastUpdateMillis = now;
                double ratio = (double) processed / total;
                long elapsed = now - startMillis;
                Platform.runLater(() -> {
                    progressBar.setProgress(ratio);
                    remainingLabel.setText(I18n.tr("cleanAccountsRemaining", formatRemaining(elapsed, ratio)));
                });
            }

            @Override
            public void onLog(String message) {
                Platform.runLater(() -> appendLog(message));
            }

            @Override
            public boolean isStopRequested() {
                return stopRequested;
            }
        };
        try {
            if (normalizeNames) {
                appendLogOnFx(I18n.tr("cleanLogPhaseNormalize"));
                AccountCleanupService.CleanupReport report = service.run(listener);
                appendLogOnFx(report.stoppedEarly()
                        ? I18n.tr("cleanLogStopped")
                        : I18n.tr("cleanLogSummary", report.renamedAccounts(), report.removedDuplicates()));
            } else {
                appendLogOnFx(I18n.tr("clearDuplicatesLogPhase"));
                int removedDuplicates = service.removeDuplicatesOnly(listener);
                appendLogOnFx(I18n.tr("clearDuplicatesLogSummary", removedDuplicates));
            }
        } catch (Exception e) {
            String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            appendLogOnFx(I18n.tr("reloadExportLogFailed", reason));
        }
        Platform.runLater(() -> {
            progressBar.setProgress(1.0);
            remainingLabel.setText("");
        });
    }

    private String formatRemaining(long elapsedMillis, double ratio) {
        if (ratio < 0.02) {
            return "";
        }
        long remainingMillis = (long) (elapsedMillis / ratio) - elapsedMillis;
        long minutes = remainingMillis / 60000;
        long seconds = (remainingMillis % 60000) / 1000;
        return minutes > 0 ? minutes + "m " + seconds + "s" : seconds + "s";
    }

    private void appendLogOnFx(String message) {
        Platform.runLater(() -> appendLog(message));
    }

    private void appendLog(String message) {
        logBuilder.append(message).append(System.lineSeparator());
        logArea.appendText(message + System.lineSeparator());
    }

    private void copyLogToClipboard() {
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(logBuilder.toString());
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
    }

    private void exportLogToFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.tr("reloadExportLog"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
        fileChooser.setInitialFileName("uiptv_clean_log_" + formatter.format(LocalDateTime.now()) + ".txt");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.tr("reloadExportLogFileExtension"), "*.txt"));
        File target = fileChooser.showSaveDialog(stage);
        if (target == null) {
            return;
        }
        try {
            java.nio.file.Files.writeString(target.toPath(), logBuilder.toString(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            com.uiptv.widget.UIptvAlert.showErrorAlert(I18n.tr("reloadExportLogFailed", e.getMessage()));
        }
    }
}
