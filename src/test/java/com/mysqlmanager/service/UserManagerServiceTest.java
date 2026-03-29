package com.mysqlmanager.service;

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
class UserManagerServiceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private UserManagerService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new UserManagerService(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
    }

    // --- validateIdentifier ---

    @Test
    void invalidIdentifierRejectsNull() {
        assertThatThrownBy(() -> service.showGrants(null, "localhost"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidIdentifierRejectsInjection() {
        assertThatThrownBy(() -> service.showGrants("user'; DROP", "localhost"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- listUsers ---

    @Test
    void listUsersReturnsAll() throws Exception {
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("user")).thenReturn("root", "mario");
        when(resultSet.getString("host")).thenReturn("localhost", "%");

        List<UserManagerService.MysqlUser> users = service.listUsers();

        assertThat(users).hasSize(2);
        assertThat(users.get(0).user()).isEqualTo("root");
        assertThat(users.get(1).host()).isEqualTo("%");
    }

    // --- showGrants ---

    @Test
    void showGrantsReturnsList() throws Exception {
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString(1)).thenReturn("GRANT ALL ON *.* TO 'root'@'localhost'");

        List<String> grants = service.showGrants("root", "localhost");

        assertThat(grants).containsExactly("GRANT ALL ON *.* TO 'root'@'localhost'");
    }

    // --- createUser ---

    @Test
    void createUserExecutesPreparedStatement() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        service.createUser("mario", "localhost", "Pass@1");

        verify(preparedStatement).setString(1, "mario");
        verify(preparedStatement).setString(2, "localhost");
        verify(preparedStatement).setString(3, "Pass@1");
        verify(preparedStatement).execute();
    }

    @Test
    void createUserRejectsInvalidUser() {
        assertThatThrownBy(() -> service.createUser("mario; DROP", "localhost", "pass"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- listUsersForDatabase ---

    @Test
    void listUsersForDatabaseReturnsList() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("user")).thenReturn("mario");
        when(resultSet.getString("host")).thenReturn("localhost");

        List<UserManagerService.MysqlUser> users = service.listUsersForDatabase("mydb");

        assertThat(users).hasSize(1);
        assertThat(users.get(0).user()).isEqualTo("mario");
    }

    // --- changePassword ---

    @Test
    void changePasswordExecutesAlterAndFlush() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(connection.createStatement()).thenReturn(statement);

        service.changePassword("mario", "localhost", "NewPass@1");

        verify(preparedStatement).setString(1, "mario");
        verify(preparedStatement).setString(2, "localhost");
        verify(preparedStatement).setString(3, "NewPass@1");
        verify(statement).execute("FLUSH PRIVILEGES");
    }

    // --- dropUser ---

    @Test
    void dropUserExecutesDdl() throws Exception {
        when(connection.createStatement()).thenReturn(statement);

        service.dropUser("mario", "localhost");

        verify(statement).execute("DROP USER 'mario'@'localhost'");
    }

    @Test
    void dropUserRejectsInvalidIdentifier() {
        assertThatThrownBy(() -> service.dropUser(null, "localhost"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- grantPrivileges ---

    @Test
    void grantPrivilegesAllPrivileges() throws Exception {
        when(connection.createStatement()).thenReturn(statement);

        service.grantPrivileges("mario", "localhost", "mydb", "ALL PRIVILEGES");

        verify(statement).execute("GRANT ALL PRIVILEGES ON `mydb`.* TO 'mario'@'localhost'");
        verify(statement).execute("FLUSH PRIVILEGES");
    }

    @Test
    void grantPrivilegesSpecific() throws Exception {
        when(connection.createStatement()).thenReturn(statement);

        service.grantPrivileges("mario", "localhost", "mydb", "SELECT,INSERT");

        verify(statement).execute("GRANT SELECT,INSERT ON `mydb`.* TO 'mario'@'localhost'");
    }

    @Test
    void grantPrivilegesRejectsInvalidPrivileges() {
        assertThatThrownBy(() -> service.grantPrivileges("mario", "localhost", "mydb", "SUPERUSER"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void grantPrivilegesRejectsNullPrivileges() {
        assertThatThrownBy(() -> service.grantPrivileges("mario", "localhost", "mydb", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- revokePrivileges ---

    @Test
    void revokePrivilegesExecutes() throws Exception {
        when(connection.createStatement()).thenReturn(statement);

        service.revokePrivileges("mario", "localhost", "mydb", "SELECT");

        verify(statement).execute("REVOKE SELECT ON `mydb`.* FROM 'mario'@'localhost'");
        verify(statement).execute("FLUSH PRIVILEGES");
    }

    // --- MysqlUser record ---

    @Test
    void mysqlUserRecord() {
        UserManagerService.MysqlUser u = new UserManagerService.MysqlUser("root", "localhost");
        assertThat(u.user()).isEqualTo("root");
        assertThat(u.host()).isEqualTo("localhost");
    }
}
