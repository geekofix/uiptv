package com.uiptv.ui;

import com.uiptv.db.AccountDb;
import com.uiptv.db.ChannelDb;
import com.uiptv.db.SeriesChannelDb;
import com.uiptv.db.VodChannelDb;
import com.uiptv.model.Account;
import com.uiptv.model.AccountInfo;
import com.uiptv.service.AccountChangeListener;
import com.uiptv.service.AccountInfoService;
import com.uiptv.service.AccountService;
import com.uiptv.util.AccountNameFormats;
import com.uiptv.util.I18n;
import com.uiptv.util.StringUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AccountInfoUI extends VBox {

    private record AccountInfoRow(String accountName, String baseName, int pinLevel, String type,
                                  String status, String expires, String liveChannels,
                                  String vod, String series) {
    }

    private static final Comparator<AccountInfoRow> ROW_ORDER =
            Comparator.comparingInt(AccountInfoRow::pinLevel).reversed()
                    .thenComparing(row -> row.baseName(), String.CASE_INSENSITIVE_ORDER);
    private final TableView<AccountInfoRow> table = new TableView<>();
    private final ObservableList<AccountInfoRow> rows = FXCollections.observableArrayList();
    private final AccountChangeListener accountChangeListener = _ -> Platform.runLater(this::refresh);
    private final java.util.concurrent.atomic.AtomicBoolean refreshInProgress = new java.util.concurrent.atomic.AtomicBoolean(false);

    public AccountInfoUI() {
        setSpacing(5);
        setPadding(new Insets(5));

        TableColumn<AccountInfoRow, String> accountColumn = column("accountInfoColAccount", row -> new SimpleStringProperty(row.accountName()));
        TableColumn<AccountInfoRow, String> typeColumn = column("accountInfoColType", row -> new SimpleStringProperty(row.type()));
        TableColumn<AccountInfoRow, String> statusColumn = column("accountInfoColStatus", row -> new SimpleStringProperty(row.status()));
        statusColumn.setCellFactory(_ -> createStatusCell());
        TableColumn<AccountInfoRow, String> expiresColumn = column("accountInfoColExpires", row -> new SimpleStringProperty(row.expires()));
        TableColumn<AccountInfoRow, String> liveColumn = column("accountInfoColLiveTv", row -> new SimpleStringProperty(row.liveChannels()));
        TableColumn<AccountInfoRow, String> vodColumn = column("accountInfoColVod", row -> new SimpleStringProperty(row.vod()));
        TableColumn<AccountInfoRow, String> seriesColumn = column("accountInfoColSeries", row -> new SimpleStringProperty(row.series()));

        table.getColumns().setAll(accountColumn, typeColumn, statusColumn, expiresColumn, liveColumn, vodColumn, seriesColumn);
        table.setItems(rows);
        table.setPlaceholder(new Label(I18n.tr("accountInfoEmpty")));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().add(table);
    }

    public void registerListeners() {
        AccountService.getInstance().addChangeListener(accountChangeListener);
    }

    public void unregisterListeners() {
        AccountService.getInstance().removeChangeListener(accountChangeListener);
    }

    public void refresh() {
        if (refreshInProgress.getAndSet(true)) {
            return;
        }
        Thread worker = new Thread(() -> {
            try {
                List<AccountInfoRow> newRows = collectRows();
                Platform.runLater(() -> {
                    rows.setAll(newRows);
                    refreshInProgress.set(false);
                });
            } catch (Exception e) {
                refreshInProgress.set(false);
            }
        }, "account-info-refresh");
        worker.setDaemon(true);
        worker.start();
    }

    private List<AccountInfoRow> collectRows() {
        List<AccountInfoRow> newRows = new ArrayList<>();
        for (Account account : AccountDb.get().getAccounts()) {
            String accountName = account.getAccountName() == null ? "" : account.getAccountName();
            AccountInfo info = AccountInfoService.getInstance().getByAccountId(account.getDbId());
            AccountNameFormats.SuffixMatch match = AccountNameFormats.matchSuffix(accountName);
            newRows.add(new AccountInfoRow(
                    accountName,
                    match != null ? match.baseName() : accountName,
                    account.getPinLevel(),
                    account.getType() == null ? "-" : account.getType().name(),
                    info != null && info.getAccountStatus() != null ? info.getAccountStatus().name() : "-",
                    info != null && StringUtils.isNotBlank(info.getExpireDate())
                            ? AccountInfoUiUtil.formatDate(info.getExpireDate()) : "-",
                    numberOrDash(ChannelDb.get().getChannelCountForAccount(account.getDbId())),
                    numberOrDash(VodChannelDb.get().getChannelCountForAccount(account.getDbId())),
                    numberOrDash(SeriesChannelDb.get().getChannelCountForAccount(account.getDbId()))
            ));
        }
        newRows.sort(ROW_ORDER);
        return newRows;
    }

    private static String numberOrDash(int count) {
        return count > 0 ? String.valueOf(count) : "-";
    }

    private TableColumn<AccountInfoRow, String> column(String titleKey,
            Callback<AccountInfoRow, SimpleStringProperty> valueFactory) {
        TableColumn<AccountInfoRow, String> column = new TableColumn<>(I18n.tr(titleKey));
        column.setCellValueFactory(data -> valueFactory.call(data.getValue()));
        column.setSortable(false);
        return column;
    }

    private TableCell<AccountInfoRow, String> createStatusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTextFill(null);
                    return;
                }
                setText(item);
                AccountInfoUiUtil.StatusState state = AccountInfoUiUtil.resolveStatusState(item);
                String color = AccountInfoUiUtil.colorForStatus(state);
                setTextFill(color != null ? Color.web(color) : null);
            }
        };
    }
}
