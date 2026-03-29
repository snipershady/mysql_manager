package com.mysqlmanager.service;

import com.mysqlmanager.dto.QueryResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DatabaseManagerServiceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private DatabaseMetaData metaData;

    private DatabaseManagerService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new DatabaseManagerService(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
    }

    // --- validateIdentifier ---

    @Test
    void invalidIdentifierNull() throws Exception {
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getTables(null, null, "%", new String[]{"TABLE", "VIEW"})).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertThatThrownBy(() -> service.listTables(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidIdentifierSpecialChars() {
        assertThatThrownBy(() -> service.listTables("db;DROP TABLE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- listDatabases ---

    @Test
    void listDatabasesExcludesSystem() throws Exception {
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getCatalogs()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, true, false);
        when(resultSet.getString(1)).thenReturn("mydb", "information_schema", "sys");

        List<String> dbs = service.listDatabases(false);

        assertThat(dbs).containsExactly("mydb");
    }

    @Test
    void listDatabasesIncludesSystem() throws Exception {
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getCatalogs()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString(1)).thenReturn("mydb", "mysql");

        List<String> dbs = service.listDatabases(true);

        assertThat(dbs).containsExactly("mydb", "mysql");
    }

    // --- listTables ---

    @Test
    void listTablesReturnsTableNames() throws Exception {
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getTables("mydb", null, "%", new String[]{"TABLE", "VIEW"})).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("TABLE_NAME")).thenReturn("users", "orders");

        List<String> tables = service.listTables("mydb");

        assertThat(tables).containsExactly("users", "orders");
    }

    @Test
    void listTablesRejectsInvalidIdentifier() {
        assertThatThrownBy(() -> service.listTables("../hack"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- describeTable ---

    @Test
    void describeTableReturnsColumnInfoList() throws Exception {
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getColumns("mydb", null, "users", "%")).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("COLUMN_NAME")).thenReturn("id");
        when(resultSet.getString("TYPE_NAME")).thenReturn("INT");
        when(resultSet.getInt("COLUMN_SIZE")).thenReturn(11);
        when(resultSet.getString("IS_NULLABLE")).thenReturn("NO");
        when(resultSet.getString("COLUMN_DEF")).thenReturn(null);

        List<DatabaseManagerService.ColumnInfo> cols = service.describeTable("mydb", "users");

        assertThat(cols).hasSize(1);
        assertThat(cols.get(0).name()).isEqualTo("id");
        assertThat(cols.get(0).type()).isEqualTo("INT");
        assertThat(cols.get(0).nullable()).isEqualTo("NO");
    }

    // --- executeQuery ---

    @Test
    void executeQuerySelectReturnsResultDto() throws Exception {
        ResultSetMetaData rsMeta = mock(ResultSetMetaData.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);
        when(statement.getResultSet()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(rsMeta);
        when(rsMeta.getColumnCount()).thenReturn(2);
        when(rsMeta.getColumnLabel(1)).thenReturn("id");
        when(rsMeta.getColumnLabel(2)).thenReturn("name");
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getObject(1)).thenReturn(1);
        when(resultSet.getObject(2)).thenReturn("Alice");

        QueryResultDto result = service.executeQuery("mydb", "SELECT id, name FROM t", true);

        assertThat(result.getColumns()).containsExactly("id", "name");
        assertThat(result.getRows()).hasSize(1);
        assertThat(result.isUpdateQuery()).isFalse();
    }

    @Test
    void executeQueryUpdateReturnsAffectedRows() throws Exception {
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(false);
        when(statement.getUpdateCount()).thenReturn(3);

        QueryResultDto result = service.executeQuery("mydb", "DELETE FROM t WHERE id=1", true);

        assertThat(result.isUpdateQuery()).isTrue();
        assertThat(result.getAffectedRows()).isEqualTo(3);
    }

    @Test
    void executeQueryOperatorBlocksDml() {
        assertThatThrownBy(() -> service.executeQuery("mydb", "DELETE FROM t", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Permesso negato");
    }

    @Test
    void executeQueryOperatorAllowsSelect() throws Exception {
        ResultSetMetaData rsMeta = mock(ResultSetMetaData.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);
        when(statement.getResultSet()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(rsMeta);
        when(rsMeta.getColumnCount()).thenReturn(0);
        when(resultSet.next()).thenReturn(false);

        QueryResultDto result = service.executeQuery("mydb", "SELECT 1", false);

        assertThat(result.isUpdateQuery()).isFalse();
    }

    @Test
    void executeQueryOperatorAllowsShow() throws Exception {
        ResultSetMetaData rsMeta = mock(ResultSetMetaData.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);
        when(statement.getResultSet()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(rsMeta);
        when(rsMeta.getColumnCount()).thenReturn(0);
        when(resultSet.next()).thenReturn(false);

        QueryResultDto result = service.executeQuery("mydb", "SHOW TABLES", false);

        assertThat(result).isNotNull();
    }

    @Test
    void executeQueryOperatorAllowsDescribe() throws Exception {
        ResultSetMetaData rsMeta = mock(ResultSetMetaData.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);
        when(statement.getResultSet()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(rsMeta);
        when(rsMeta.getColumnCount()).thenReturn(0);
        when(resultSet.next()).thenReturn(false);

        QueryResultDto result = service.executeQuery("mydb", "DESCRIBE users", false);

        assertThat(result).isNotNull();
    }

    // --- createDatabase ---

    @Test
    void createDatabaseExecutesDdl() throws Exception {
        when(connection.createStatement()).thenReturn(statement);

        service.createDatabase("newdb", "utf8mb4", "utf8mb4_unicode_ci");

        verify(statement).execute("CREATE DATABASE `newdb` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
    }

    @Test
    void createDatabaseUsesDefaultCharsetForInvalidInput() throws Exception {
        when(connection.createStatement()).thenReturn(statement);

        service.createDatabase("newdb", "invalid charset!", "invalid collation!");

        verify(statement).execute("CREATE DATABASE `newdb` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
    }

    @Test
    void createDatabaseRejectsInvalidName() {
        assertThatThrownBy(() -> service.createDatabase("db;DROP", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- dropDatabase ---

    @Test
    void dropDatabaseExecutesDdl() throws Exception {
        when(connection.createStatement()).thenReturn(statement);

        service.dropDatabase("olddb");

        verify(statement).execute("DROP DATABASE `olddb`");
    }

    @Test
    void dropDatabaseRejectsInvalidName() {
        assertThatThrownBy(() -> service.dropDatabase("../etc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- showCreateTable ---

    @Test
    void showCreateTableReturnsDefinition() throws Exception {
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(2)).thenReturn("CREATE TABLE `users` (...)");

        String ddl = service.showCreateTable("mydb", "users");

        assertThat(ddl).contains("CREATE TABLE");
    }

    @Test
    void showCreateTableReturnsEmptyWhenNoResult() throws Exception {
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        String ddl = service.showCreateTable("mydb", "users");

        assertThat(ddl).isEmpty();
    }

    // --- ColumnInfo record ---

    @Test
    void columnInfoRecord() {
        DatabaseManagerService.ColumnInfo col = new DatabaseManagerService.ColumnInfo("id", "INT", 11, "NO", null);
        assertThat(col.name()).isEqualTo("id");
        assertThat(col.type()).isEqualTo("INT");
        assertThat(col.size()).isEqualTo(11);
        assertThat(col.nullable()).isEqualTo("NO");
        assertThat(col.defaultValue()).isNull();
    }
}
