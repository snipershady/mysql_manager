package com.mysqlmanager.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogTest {

    @Test
    void defaultValues() {
        AuditLog log = new AuditLog();
        assertThat(log.getExecutedAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(log.isSuccess()).isFalse();
        assertThat(log.getExecutionTimeMs()).isZero();
    }

    @Test
    void settersAndGetters() {
        AuditLog log = new AuditLog();
        log.setId(42L);
        log.setUsername("admin");
        log.setClientIp("192.168.1.1");
        log.setTargetDatabase("mydb");
        log.setSqlText("SELECT 1");
        log.setExecutionTimeMs(123L);
        log.setSuccess(true);
        log.setErrorMessage(null);

        assertThat(log.getId()).isEqualTo(42L);
        assertThat(log.getUsername()).isEqualTo("admin");
        assertThat(log.getClientIp()).isEqualTo("192.168.1.1");
        assertThat(log.getTargetDatabase()).isEqualTo("mydb");
        assertThat(log.getSqlText()).isEqualTo("SELECT 1");
        assertThat(log.getExecutionTimeMs()).isEqualTo(123L);
        assertThat(log.isSuccess()).isTrue();
        assertThat(log.getErrorMessage()).isNull();
    }

    @Test
    void errorMessage() {
        AuditLog log = new AuditLog();
        log.setSuccess(false);
        log.setErrorMessage("Syntax error");
        assertThat(log.getErrorMessage()).isEqualTo("Syntax error");
    }
}
