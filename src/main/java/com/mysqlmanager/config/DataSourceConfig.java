package com.mysqlmanager.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
public class DataSourceConfig {

    @Value("${mysql.manager.host:127.0.0.1}")
    private String host;

    @Value("${mysql.manager.port:3306}")
    private int port;

    @Value("${mysql.manager.user:root}")
    private String user;

    @Value("${mysql.manager.password:}")
    private String password;

    /**
     * DataSource primario per lo schema dell'applicazione (mysql_manager_app).
     * Crea il database se non esiste prima di avviare il pool Hikari,
     * compatibile con MySQL 8.x e 9.x.
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        String initUrl = String.format(
            "jdbc:mysql://%s:%d/?sslMode=DISABLED&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=10000",
            host, port);

        try (Connection conn = DriverManager.getConnection(initUrl, user, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE IF NOT EXISTS mysql_manager_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLException e) {
            throw new RuntimeException("Impossibile inizializzare il database mysql_manager_app: " + e.getMessage(), e);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format(
            "jdbc:mysql://%s:%d/mysql_manager_app?sslMode=DISABLED&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=10000&socketTimeout=60000",
            host, port));
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10000);
        config.setPoolName("AppMySQLPool");
        return new HikariDataSource(config);
    }

    /**
     * DataSource separato per la gestione dei database MySQL target.
     * Non ha un database selezionato, permette operazioni su qualsiasi DB.
     */
    @Bean(name = "managedMysqlDataSource")
    public DataSource managedMysqlDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format(
            "jdbc:mysql://%s:%d/?sslMode=DISABLED&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=10000&socketTimeout=60000",
            host, port));
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(5000);
        config.setPoolName("ManagedMySQLPool");
        return new HikariDataSource(config);
    }
}
