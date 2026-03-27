package com.mysqlmanager.service;

import com.mysqlmanager.dto.QueryResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DatabaseManagerService {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z0-9_$]{1,64}$");
    private static final Set<String> SYSTEM_DATABASES = Set.of(
            "information_schema", "performance_schema", "sys", "mysql"
    );

    @Qualifier("managedMysqlDataSource")
    private final DataSource managedDataSource;

    private void validateIdentifier(String name) {
        if (name == null || !IDENTIFIER_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Identificatore non valido: " + name);
        }
    }

    public List<String> listDatabases(boolean includeSystem) throws SQLException {
        List<String> databases = new ArrayList<>();
        try (Connection conn = managedDataSource.getConnection();
             ResultSet rs = conn.getMetaData().getCatalogs()) {
            while (rs.next()) {
                String db = rs.getString(1);
                if (includeSystem || !SYSTEM_DATABASES.contains(db.toLowerCase())) {
                    databases.add(db);
                }
            }
        }
        return databases;
    }

    public List<String> listTables(String database) throws SQLException {
        validateIdentifier(database);
        List<String> tables = new ArrayList<>();
        try (Connection conn = managedDataSource.getConnection();
             ResultSet rs = conn.getMetaData().getTables(database, null, "%", new String[]{"TABLE", "VIEW"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        return tables;
    }

    public List<ColumnInfo> describeTable(String database, String table) throws SQLException {
        validateIdentifier(database);
        validateIdentifier(table);
        List<ColumnInfo> columns = new ArrayList<>();
        try (Connection conn = managedDataSource.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(database, null, table, "%")) {
            while (rs.next()) {
                columns.add(new ColumnInfo(
                        rs.getString("COLUMN_NAME"),
                        rs.getString("TYPE_NAME"),
                        rs.getInt("COLUMN_SIZE"),
                        rs.getString("IS_NULLABLE"),
                        rs.getString("COLUMN_DEF")
                ));
            }
        }
        return columns;
    }

    public QueryResultDto executeQuery(String database, String sql, boolean adminRole) throws SQLException {
        if (!adminRole) {
            String trimmed = sql.strip().toUpperCase();
            if (!trimmed.startsWith("SELECT") && !trimmed.startsWith("SHOW") && !trimmed.startsWith("DESCRIBE")) {
                throw new IllegalArgumentException("Permesso negato: solo SELECT/SHOW/DESCRIBE consentiti per OPERATOR");
            }
        }

        validateIdentifier(database);
        long start = System.currentTimeMillis();

        try (Connection conn = managedDataSource.getConnection()) {
            conn.setCatalog(database);
            try (Statement stmt = conn.createStatement()) {
                stmt.setMaxRows(1000);
                boolean hasResultSet = stmt.execute(sql);
                long elapsed = System.currentTimeMillis() - start;

                if (hasResultSet) {
                    try (ResultSet rs = stmt.getResultSet()) {
                        ResultSetMetaData meta = rs.getMetaData();
                        int colCount = meta.getColumnCount();
                        List<String> cols = new ArrayList<>();
                        for (int i = 1; i <= colCount; i++) {
                            cols.add(meta.getColumnLabel(i));
                        }
                        List<List<Object>> rows = new ArrayList<>();
                        while (rs.next()) {
                            List<Object> row = new ArrayList<>();
                            for (int i = 1; i <= colCount; i++) {
                                row.add(rs.getObject(i));
                            }
                            rows.add(row);
                        }
                        return new QueryResultDto(cols, rows, elapsed);
                    }
                } else {
                    return new QueryResultDto(stmt.getUpdateCount(), elapsed);
                }
            }
        }
    }

    public void createDatabase(String name, String charset, String collation) throws SQLException {
        validateIdentifier(name);
        String safeCharset = charset != null && IDENTIFIER_PATTERN.matcher(charset).matches() ? charset : "utf8mb4";
        String safeCollation = collation != null && IDENTIFIER_PATTERN.matcher(collation).matches() ? collation : "utf8mb4_unicode_ci";
        String sql = "CREATE DATABASE `" + name + "` CHARACTER SET " + safeCharset + " COLLATE " + safeCollation;
        try (Connection conn = managedDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void dropDatabase(String name) throws SQLException {
        validateIdentifier(name);
        try (Connection conn = managedDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP DATABASE `" + name + "`");
        }
    }

    public String showCreateTable(String database, String table) throws SQLException {
        validateIdentifier(database);
        validateIdentifier(table);
        try (Connection conn = managedDataSource.getConnection()) {
            conn.setCatalog(database);
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE `" + table + "`")) {
                if (rs.next()) {
                    return rs.getString(2);
                }
            }
        }
        return "";
    }

    public record ColumnInfo(String name, String type, int size, String nullable, String defaultValue) {}
}
