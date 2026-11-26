package com.uiptv.ui;

import com.uiptv.model.Account;
import com.uiptv.util.I18n;

import com.uiptv.widget.SegmentedProgressBar;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ProgressDialog extends Stage {
    private static final String LOG_TEXT_STYLE_CLASS = "log-text";
    private final List<String> logLines = new ArrayList<>();
    private final List<String> statusLines = new ArrayList<>();
    private final Map<javafx.scene.control.CheckBox, Account> statusAccounts = new LinkedHashMap<>();
    private final List<javafx.scene.control.CheckBox> statusCheckBoxes = new ArrayList<>();
    private final Map<String, StatusGroup> statusGroups = new LinkedHashMap<>();
    private final Button deleteSelectedButton = new Button(I18n.tr("verifyDeleteSelected"));
    private Consumer<List<Account>> onDeleteSelectedAccounts;

    private final SegmentedProgressBar progressBar = new SegmentedProgressBar();
    private final VBox messageContainer = new VBox();
    private final ScrollPane scrollPane = new ScrollPane(messageContainer);
    private final VBox statusContainer = new VBox();
    private final ScrollPane statusScrollPane = new ScrollPane(statusContainer);
    private final VBox statusPane = new VBox(5, new Label(I18n.tr("verifyServerStatusTitle")), statusScrollPane);
    private final Button cancelButton = new Button(I18n.tr("autoCancel"));
    private final Button stopButton = new Button(I18n.tr("autoStop"));
    private final ComboBox<String> delayDropdown = new ComboBox<>();
    
    // Pause Widget Components
    private final HBox pauseWidget = new HBox(10);
    private final Line clockHand = new Line(0, 0, 0, -10);
    private final Label pauseLabel = new Label();

    public ProgressDialog(Stage owner) {
        initOwner(owner);
        initModality(Modality.NONE);
        setTitle(I18n.tr("autoVerifyingMacAddresses"));

        // Root Layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        
        // Progress Bar (Top)
        BorderPane.setMargin(progressBar, new Insets(0, 0, 10, 0));
        
        // Message Area (Center)
        scrollPane.getStyleClass().add("log-scroll-pane");
        scrollPane.setFitToWidth(true);
        messageContainer.getStyleClass().add("log-message-container");
        
        // Bottom Bar (Bottom)
        cancelButton.getStyleClass().add("dialog-button");
        stopButton.getStyleClass().add("dialog-button");

        // Delay Dropdown
        delayDropdown.setItems(FXCollections.observableArrayList("1 sec", "5 secs", "10 secs", "30 secs", "1 min", "10 mins", "30 mins"));
        delayDropdown.setValue("10 secs");

        // Pause Widget
        Circle clockFace = new Circle(12);
        clockFace.getStyleClass().add("clock-face");
        clockHand.getStyleClass().add("clock-hand");
        StackPane clockIcon = new StackPane(clockFace, clockHand);
        pauseWidget.getChildren().addAll(clockIcon, pauseLabel);
        pauseWidget.setAlignment(Pos.CENTER_LEFT);
        pauseWidget.setVisible(false);

        HBox bottomBar = new HBox(10);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(10, 0, 0, 0));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bottomBar.getChildren().addAll(pauseWidget, new Label(I18n.tr("autoDelay")), delayDropdown, stopButton, spacer, cancelButton);

        // Server Status Pane (hidden until the first server error is reported)
        statusContainer.getStyleClass().add("log-message-container");
        statusScrollPane.getStyleClass().add("log-scroll-pane");
        statusScrollPane.setFitToWidth(true);
        statusPane.setVisible(false);
        statusPane.setMinWidth(250);
        VBox.setVgrow(statusScrollPane, Priority.ALWAYS);
        statusPane.getChildren().add(buildStatusPaneButtons());

        HBox logArea = new HBox(10, buildLogPane(), statusPane);
        logArea.getChildren().forEach(pane -> HBox.setHgrow(pane, Priority.ALWAYS));

        // Assemble Root
        root.setTop(progressBar);
        root.setCenter(logArea);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root, 900, 620); // Increased width for dropdown
        I18n.applySceneOrientation(scene);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        setScene(scene);
        
        progressBar.prefWidthProperty().bind(scene.widthProperty().subtract(45));
    }

    public void setSelectedDelay(String value) {
        if (value != null && delayDropdown.getItems().contains(value)) {
            delayDropdown.setValue(value);
        }
    }

    public long getSelectedDelayMillis() {
        String selected = delayDropdown.getValue();
        if (selected == null) return 10000; // Default 10s
        
        String[] parts = selected.split(" ");
        int value = Integer.parseInt(parts[0]);
        
        switch (parts[1]) {
            case "sec":
            case "secs":
                return value * 1000L;
            case "min":
            case "mins":
                return value * 60 * 1000L;
            default:
                return 10000L;
        }
    }

    public void setTotal(int total) {
        progressBar.setTotal(total);
    }

    public void addResult(boolean isValid) {
        progressBar.addResult(isValid);
    }

    public void addErrorResult() {
        progressBar.addErrorResult();
    }

    private VBox buildLogPane() {
        HBox logButtons = new HBox(10, buildCopyButton(logLines), buildExportButton(logLines, "uiptv_verify_log_"));
        VBox logPane = new VBox(5, scrollPane, logButtons);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return logPane;
    }

    private HBox buildStatusPaneButtons() {
        javafx.scene.control.CheckBox selectAllCheckBox = new javafx.scene.control.CheckBox(I18n.tr("verifySelectAll"));
        selectAllCheckBox.setOnAction(event -> {
            boolean select = selectAllCheckBox.isSelected();
            statusCheckBoxes.forEach(checkBox -> checkBox.setSelected(select));
            statusGroups.values().forEach(this::updateGroupHeaderState);
            updateDeleteSelectedButtonState();
        });
        deleteSelectedButton.disableProperty().set(true);
        deleteSelectedButton.setOnAction(event -> {
            List<Account> selectedAccounts = statusCheckBoxes.stream()
                    .filter(javafx.scene.control.CheckBox::isSelected)
                    .map(statusAccounts::get)
                    .distinct()
                    .toList();
            if (!selectedAccounts.isEmpty() && onDeleteSelectedAccounts != null) {
                onDeleteSelectedAccounts.accept(selectedAccounts);
            }
        });
        return new HBox(10, selectAllCheckBox, deleteSelectedButton, buildCopyButton(statusLines), buildExportButton(statusLines, "uiptv_verify_status_"));
    }

    private Button buildCopyButton(List<String> lines) {
        Button copyButton = new Button(I18n.tr("autoCopyLog"));
        copyButton.setOnAction(event -> {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(String.join("\n", lines));
            clipboard.setContent(content);
        });
        return copyButton;
    }

    private Button buildExportButton(List<String> lines, String filePrefix) {
        Button exportButton = new Button(I18n.tr("reloadExportLog"));
        exportButton.setOnAction(event -> exportLinesToFile(lines, filePrefix));
        return exportButton;
    }

    private void exportLinesToFile(List<String> lines, String filePrefix) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.tr("reloadExportLog"));
        fileChooser.setInitialFileName(buildExportFileName(filePrefix));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.tr("reloadExportLogFileExtension"), "*.txt"));
        java.io.File target = fileChooser.showSaveDialog(this);
        if (target == null) {
            return;
        }
        try {
            java.nio.file.Files.writeString(target.toPath(), String.join("\n", lines), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            com.uiptv.widget.UIptvAlert.showErrorAlert(I18n.tr("reloadExportLogFailed", e.getMessage()));
        }
    }

    private String buildExportFileName(String filePrefix) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
        return filePrefix + formatter.format(LocalDateTime.now()) + ".txt";
    }

    public void addProgressText(String text) {
        Platform.runLater(() -> {
            boolean pinnedToBottom = isPinnedToBottom(scrollPane);
            if (text.startsWith("[UPDATE_LAST]")) {
                updateLastLine(text.substring(13));
            } else {
                logLines.add(text);
                messageContainer.getChildren().add(createStyledText(text));
            }
            if (pinnedToBottom) {
                scrollPane.setVvalue(1.0);
            }
        });
    }

    private boolean isPinnedToBottom(ScrollPane pane) {
        return pane.getVmax() == 0 || pane.getVvalue() >= 0.99;
    }

    public void setOnDeleteSelectedAccounts(Consumer<List<Account>> consumer) {
        this.onDeleteSelectedAccounts = consumer;
    }

    public void addProblemEntry(Account account, String label, String message, String groupKey) {
        Platform.runLater(() -> {
            statusPane.setVisible(true);
            boolean pinnedToBottom = isPinnedToBottom(statusScrollPane);
            statusLines.add(account.getAccountName() + " — " + label + " — " + message);
            StatusGroup group = statusGroups.computeIfAbsent(groupKey, this::createStatusGroup);
            javafx.scene.control.CheckBox checkBox = new javafx.scene.control.CheckBox();
            statusAccounts.put(checkBox, account);
            statusCheckBoxes.add(checkBox);
            group.entries.add(checkBox);
            checkBox.setOnAction(event -> {
                updateDeleteSelectedButtonState();
                updateGroupHeaderState(group);
            });
            HBox entry = new HBox(8, checkBox, createStyledText(account.getAccountName() + " — " + label));
            group.entryBox.getChildren().add(entry);
            group.countLabel.setText("(" + group.entries.size() + ")");
            if (pinnedToBottom) {
                statusScrollPane.setVvalue(1.0);
            }
        });
    }

    private final class StatusGroup {
        private final javafx.scene.control.CheckBox headerCheckBox = new javafx.scene.control.CheckBox();
        private final Label countLabel = new Label();
        private final VBox entryBox = new VBox(3);
        private final List<javafx.scene.control.CheckBox> entries = new ArrayList<>();
    }

    private StatusGroup createStatusGroup(String message) {
        StatusGroup group = new StatusGroup();
        group.headerCheckBox.setText(message);
        group.headerCheckBox.setAllowIndeterminate(true);
        group.headerCheckBox.setOnAction(event -> {
            group.entries.forEach(checkBox -> checkBox.setSelected(group.headerCheckBox.isSelected()));
            updateDeleteSelectedButtonState();
        });
        HBox groupHeader = new HBox(6, group.headerCheckBox, group.countLabel);
        group.entryBox.setPadding(new Insets(0, 0, 0, 20));
        VBox groupBox = new VBox(3, groupHeader, group.entryBox);
        statusContainer.getChildren().add(groupBox);
        return group;
    }

    private void updateGroupHeaderState(StatusGroup group) {
        if (group.entries.isEmpty()) {
            return;
        }
        boolean allSelected = group.entries.stream().allMatch(javafx.scene.control.CheckBox::isSelected);
        boolean noneSelected = group.entries.stream().noneMatch(javafx.scene.control.CheckBox::isSelected);
        group.headerCheckBox.setIndeterminate(!allSelected && !noneSelected);
        group.headerCheckBox.setSelected(allSelected);
    }

    private void updateDeleteSelectedButtonState() {
        deleteSelectedButton.disableProperty().set(
                statusCheckBoxes.stream().noneMatch(javafx.scene.control.CheckBox::isSelected));
    }

    public void setPauseStatus(int secondsRemaining, int totalSeconds) {
        Platform.runLater(() -> {
            if (secondsRemaining > 0) {
                pauseWidget.setVisible(true);
                pauseLabel.setText(I18n.tr("autoPausedSeconds", secondsRemaining));
                double rotation = ((double)(totalSeconds - secondsRemaining) / totalSeconds) * 360;
                clockHand.setRotate(rotation);
            } else {
                pauseWidget.setVisible(false);
            }
        });
    }

    private void updateLastLine(String text) {
        if (!logLines.isEmpty()) {
            logLines.set(logLines.size() - 1, text);
        }
        if (!messageContainer.getChildren().isEmpty()) {
            int lastIndex = messageContainer.getChildren().size() - 1;
            Node lastNode = messageContainer.getChildren().get(lastIndex);
            if (lastNode instanceof TextFlow textFlow) {
                textFlow.getChildren().setAll(new Text(text));
            }
        } else {
            logLines.add(text);
            messageContainer.getChildren().add(createStyledText(text));
        }
    }

    private TextFlow createStyledText(String text) {
        TextFlow textFlow = new TextFlow();
        List<Text> texts = new ArrayList<>();

        if (text.contains("[VALID]")) {
            String[] parts = text.split("\\[VALID\\]", 2);
            Text part1 = new Text(parts[0]);
            part1.getStyleClass().add(LOG_TEXT_STYLE_CLASS);
            texts.add(part1);
            Text validText = new Text(parts[1]);
            validText.getStyleClass().add("valid-text");
            texts.add(validText);
        } else if (text.contains("[INVALID]")) {
            String[] parts = text.split("\\[INVALID\\]", 2);
            Text part1 = new Text(parts[0]);
            part1.getStyleClass().add(LOG_TEXT_STYLE_CLASS);
            texts.add(part1);
            Text invalidText = new Text(parts[1]);
            invalidText.getStyleClass().add("invalid-text");
            texts.add(invalidText);
        } else {
            Text part1 = new Text(text);
            part1.getStyleClass().add(LOG_TEXT_STYLE_CLASS);
            texts.add(part1);
        }

        textFlow.getChildren().addAll(texts);
        return textFlow;
    }

    public void setOnClose(Runnable action) {
        cancelButton.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to cancel the verification process? No changes will be saved.", ButtonType.YES, ButtonType.NO);
            alert.initOwner(this);
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    action.run();
                    close();
                }
            });
        });

        setOnCloseRequest(event -> {
            event.consume();
            cancelButton.fire();
        });
    }

    public void setOnStop(Runnable action) {
        stopButton.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to stop? Invalid MACs found so far will be processed.", ButtonType.YES, ButtonType.NO);
            alert.initOwner(this);
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    action.run();
                }
            });
        });
    }

    public void markCompleted() {
        stopButton.setDisable(true);
        cancelButton.setText(I18n.tr("commonClose"));
        cancelButton.setOnAction(event -> close());
        setOnCloseRequest(event -> close());
    }
}
