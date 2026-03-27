package com.mysqlmanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserManagerService {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z0-9_$%@.]{1,64}$");

    @Qualifier("managedMysqlDataSource")
    private final DataSource managedDataSource;

    private void validateIdentifier(String name) {
        if (name == null || !IDENTIFIER_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Identificatore non valido: " + name);
        }
    }

    public List<MysqlUser> listUsers() throws SQLException {
        List<MysqlUser> users = new ArrayList<>();
        try (Connection conn = managedDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT user, host FROM mysql.user ORDER BY user, host")) {
            while (rs.next()) {
                users.add(new MysqlUser(rs.getString("user"), rs.getString("host")));
            }
        }
        return users;
    }

    public List<String> showGrants(String user, String host) throws SQLException {
        validateIdentifier(user);
        validateIdentifier(host);
        List<String> grants = new ArrayList<>();
        try (Connection conn = managedDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW GRANTS FOR '" + user + "'@'" + host + "'")) {
            while (rs.next()) {
                grants.add(rs.getString(1));
            }
        }
        return grants;
    }

    public void createUser(String user, String host, String password) throws SQLException {
        validateIdentifier(user);
        validateIdentifier(host);
        try (Connection conn = managedDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "CREATE USER ?@? IDENTIFIED BY ?")) {
            ps.setString(1, user);
            ps.setString(2, host);
            ps.setString(3, password);
            ps.execute();
        }
    }

    public List<MysqlUser> listUsersForDatabase(String database) throws SQLException {
        validateIdentifier(database);
        List<MysqlUser> users = new ArrayList<>();
        try (Connection conn = managedDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT DISTINCT user, host FROM mysql.db WHERE db = ? ORDER BY user, host")) {
            ps.setString(1, database);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(new MysqlUser(rs.getString("user"), rs.getString("host")));
                }
            }
        }
        return users;
    }

    public void changePassword(String user, String host, String password) throws SQLException {
        validateIdentifier(user);
        validateIdentifier(host);
        try (Connection conn = managedDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "ALTER USER ?@? IDENTIFIED BY ?")) {
            ps.setString(1, user);
            ps.setString(2, host);
            ps.setString(3, password);
            ps.execute();
        }
        try (Connection conn = managedDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("FLUSH PRIVILEGES");
        }
    }

    public void dropUser(String user, String host) throws SQLException {
        validateIdentifier(user);
        validateIdentifier(host);
        try (Connection conn = managedDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP USER '" + user + "'@'" + host + "'");
        }
    }

    public void grantPrivileges(String user, String host, String database, String privileges) throws SQLException {
        validateIdentifier(user);
        validateIdentifier(host);
        validateIdentifier(database);
        // privileges validated against whitelist
        String safePrivileges = validatePrivileges(privileges);
        try (Connection conn = managedDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("GRANT " + safePrivileges + " ON `" + database + "`.* TO '" + user + "'@'" + host + "'");
            stmt.execute("FLUSH PRIVILEGES");
        }
    }

    public void revokePrivileges(String user, String host, String database, String privileges) throws SQLException {
        validateIdentifier(user);
        validateIdentifier(host);
        validateIdentifier(database);
        String safePrivileges = validatePrivileges(privileges);
        try (Connection conn = managedDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("REVOKE " + safePrivileges + " ON `" + database + "`.* FROM '" + user + "'@'" + host + "'");
            stmt.execute("FLUSH PRIVILEGES");
        }
    }

    private String validatePrivileges(String privileges) {
        if (privileges == null) throw new IllegalArgumentException("Privilegi non specificati");
        String upper = privileges.toUpperCase().trim();
        if (upper.equals("ALL PRIVILEGES") || upper.equals("ALL")) return "ALL PRIVILEGES";
        // Allow only known privilege keywords separated by comma
        Pattern allowed = Pattern.compile("^(SELECT|INSERT|UPDATE|DELETE|CREATE|DROP|ALTER|INDEX|REFERENCES|EXECUTE)(,\\s*(SELECT|INSERT|UPDATE|DELETE|CREATE|DROP|ALTER|INDEX|REFERENCES|EXECUTE))*$");
        if (!allowed.matcher(upper).matches()) {
            throw new IllegalArgumentException("Privilegi non validi: " + privileges);
        }
        return upper;
    }

    public record MysqlUser(String user, String host) {}
}
