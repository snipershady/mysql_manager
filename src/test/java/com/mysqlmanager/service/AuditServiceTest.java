package com.mysqlmanager.service;

import com.mysqlmanager.domain.AuditLog;
import com.mysqlmanager.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    @Test
    void logSavesEntryWithAllFields() {
        auditService.log("admin", "127.0.0.1", "mydb", "SELECT 1", 55L, true, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("admin");
        assertThat(saved.getClientIp()).isEqualTo("127.0.0.1");
        assertThat(saved.getTargetDatabase()).isEqualTo("mydb");
        assertThat(saved.getSqlText()).isEqualTo("SELECT 1");
        assertThat(saved.getExecutionTimeMs()).isEqualTo(55L);
        assertThat(saved.isSuccess()).isTrue();
        assertThat(saved.getErrorMessage()).isNull();
    }

    @Test
    void logSavesErrorEntry() {
        auditService.log("operator", "10.0.0.1", "db2", "DROP TABLE t", 10L, false, "Permission denied");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.isSuccess()).isFalse();
        assertThat(saved.getErrorMessage()).isEqualTo("Permission denied");
    }

    @Test
    void findAllDelegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(List.of(new AuditLog()));
        when(auditLogRepository.findAllByOrderByExecutedAtDesc(pageable)).thenReturn(page);

        Page<AuditLog> result = auditService.findAll(pageable);

        assertThat(result).isSameAs(page);
    }

    @Test
    void findByUserDelegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<AuditLog> page = new PageImpl<>(List.of());
        when(auditLogRepository.findByUsernameOrderByExecutedAtDesc("mario", pageable)).thenReturn(page);

        Page<AuditLog> result = auditService.findByUser("mario", pageable);

        assertThat(result).isSameAs(page);
    }
}
