package com.uiptv.service;

import com.uiptv.model.Account;
import com.uiptv.db.AccountInfoDb;
import com.uiptv.db.CategoryDb;
import com.uiptv.db.ChannelDb;
import com.uiptv.db.SeriesChannelDb;
import com.uiptv.db.VodChannelDb;
import com.uiptv.model.AccountInfo;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Set;

import static com.uiptv.util.StringUtils.isBlank;

@SuppressWarnings("java:S6548")
public class AccountInfoService {
    private final Set<Runnable> changeListeners = new CopyOnWriteArraySet<>();

    private AccountInfoService() {
    }

    private static class SingletonHelper {
        private static final AccountInfoService INSTANCE = new AccountInfoService();
    }

    public static AccountInfoService getInstance() {
        return SingletonHelper.INSTANCE;
    }

    public AccountInfo getByAccountId(String accountId) {
        if (isBlank(accountId)) {
            return null;
        }
        return AccountInfoDb.get().getByAccountId(accountId);
    }

    public void save(AccountInfo info) {
        AccountInfoDb.get().save(info);
        changeListeners.forEach(Runnable::run);
    }

    public void addChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    public void deleteByAccountId(String accountId) {
        AccountInfoDb.get().deleteByAccountId(accountId);
    }

    public void backfillMissingCountsAsync() {
        Thread thread = new Thread(() -> {
            try {
                Map<String, Integer> liveCounts = ChannelDb.get().getChannelCountByAccount();
                Map<String, Integer> vodCounts = VodChannelDb.get().getChannelCountByAccount();
                Map<String, Integer> seriesCounts = SeriesChannelDb.get().getChannelCountByAccount();
                Map<String, Integer> categoryCounts = CategoryDb.get().getCountByAccount();
                for (Account account : AccountService.getInstance().getAll().values()) {
                    String accountId = account == null ? null : account.getDbId();
                    if (isBlank(accountId)) {
                        continue;
                    }
                    AccountInfo info = getByAccountId(accountId);
                    if (info != null && hasAllCounts(info)) {
                        continue;
                    }
                    if (info == null) {
                        info = new AccountInfo();
                        info.setAccountId(accountId);
                    }
                    info.setLiveCount(liveCounts.getOrDefault(accountId, 0));
                    info.setVodCount(vodCounts.getOrDefault(accountId, 0));
                    info.setSeriesCount(seriesCounts.getOrDefault(accountId, 0));
                    info.setCategoriesCount(categoryCounts.getOrDefault(accountId, 0));
                    save(info);
                }
            } catch (Exception _) {
                // Backfill is best-effort; counts refresh anyway on the next cache reload.
            }
        }, "account-info-count-backfill");
        thread.setDaemon(true);
        thread.start();
    }

    private boolean hasAllCounts(AccountInfo info) {
        return hasCount(info.getLiveCount())
                && hasCount(info.getVodCount())
                && hasCount(info.getSeriesCount())
                && hasCount(info.getCategoriesCount());
    }

    private boolean hasCount(Integer value) {
        return value != null && value >= 0;
    }
}
