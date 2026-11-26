package com.uiptv.db;

import com.uiptv.model.Account;
import com.uiptv.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.uiptv.db.DatabaseUtils.DbTable.CATEGORY_TABLE;
import static com.uiptv.db.DatabaseUtils.insertTableSql;
import static com.uiptv.db.SQLConnection.connect;

public class CategoryDb extends BaseDb {
    private static CategoryDb instance;


    public static synchronized CategoryDb get() {
        if (instance == null) {
            instance = new CategoryDb();
        }
        return instance;
    }

    public java.util.Map<String, Integer> getCountByAccount() {
        String sql = "SELECT accountId, COUNT(*) FROM " + CATEGORY_TABLE.getTableName() + " WHERE accountId IS NOT NULL GROUP BY accountId";
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        try (Connection conn = connect(); PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getString(1), rs.getInt(2));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseAccessException("Unable to execute category count by account query", e);
        }
        return counts;
    }

    public int getCategoryCountForAccount(String accountId) {
        String sql = "SELECT COUNT(*) FROM " + CATEGORY_TABLE.getTableName() + " WHERE accountId=?";
        try (Connection conn = connect(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, accountId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseAccessException("Unable to execute category count for account query", e);
        }
        return 0;
    }

    public CategoryDb() {
        super(CATEGORY_TABLE);
    }

    public List<Category> getCategories(Account account) {
        return getAll(" WHERE accountType=? AND accountId=?", new String[]{account.getAction().name(), account.getDbId()});
    }

    public List<Category> getAllAccountCategories(String accountId) {
        return getAll(" WHERE accountId=?", new String[]{accountId});
    }

    public Category getCategoryByDbId(String dbId, Account account) {
        List<Category> categories = getAll(
                " WHERE id=? AND accountType=? AND accountId=?",
                new String[]{dbId, account.getAction().name(), account.getDbId()}
        );
        return (categories != null && !categories.isEmpty()) ? categories.get(0) : null;
    }

    public void saveAll(List<Category> categories, Account account) {
        deleteByAccount(account);
        categories.forEach(c -> insert(c, account));
    }

    public void deleteByAccount(Account account) {
        String sql = "DELETE FROM " + CATEGORY_TABLE.getTableName() + " WHERE accountId=? AND accountType=?";
        try (Connection conn = connect(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, account.getDbId());
            statement.setString(2, account.getAction().name());
            statement.execute();
        } catch (SQLException e) {
            throw new DatabaseAccessException("Unable to execute delete all query", e);
        }
    }

    public void deleteByAccountId(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return;
        }
        String sql = "DELETE FROM " + CATEGORY_TABLE.getTableName() + " WHERE accountId=?";
        try (Connection conn = connect(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, accountId);
            statement.execute();
        } catch (SQLException e) {
            throw new DatabaseAccessException("Unable to execute deleteByAccountId query", e);
        }
    }

    public void insert(Category category, Account account) {
        try (Connection conn = connect(); PreparedStatement statement = conn.prepareStatement(insertTableSql(CATEGORY_TABLE))) {
            statement.setString(1, category.getCategoryId());
            statement.setString(2, account.getDbId());
            statement.setString(3, account.getAction().name());
            statement.setString(4, category.getTitle());
            statement.setString(5, category.getAlias());
            statement.setInt(6, category.isActiveSub() ? 1 : 0);
            statement.setInt(7, category.getCensored());
            statement.execute();
        } catch (SQLException e) {
            throw new DatabaseAccessException("Unable to execute query", e);
        }
    }

    @Override
    Category populate(ResultSet resultSet) {
        Category c = new Category(nullSafeString(resultSet, "categoryId"), nullSafeString(resultSet, "title"), nullSafeString(resultSet, "alias"), "1".equals(nullSafeString(resultSet, "activeSub")), safeInteger(resultSet, "censored"));
        c.setDbId(nullSafeString(resultSet, "id"));
        return c;
    }
}
