package com.mysqlmanager.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log", schema = "mysql_manager_app")
@Getter @Setter @NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(length = 64)
    private String clientIp;

    @Column(length = 128)
    private String targetDatabase;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sqlText;

    private long executionTimeMs;

    private boolean success;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime executedAt = LocalDateTime.now();
}
