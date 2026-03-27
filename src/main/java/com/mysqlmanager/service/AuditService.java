package com.mysqlmanager.service;

import com.mysqlmanager.domain.AuditLog;
import com.mysqlmanager.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String username, String clientIp, String targetDatabase,
                    String sql, long executionTimeMs, boolean success, String errorMessage) {
        AuditLog entry = new AuditLog();
        entry.setUsername(username);
        entry.setClientIp(clientIp);
        entry.setTargetDatabase(targetDatabase);
        entry.setSqlText(sql);
        entry.setExecutionTimeMs(executionTimeMs);
        entry.setSuccess(success);
        entry.setErrorMessage(errorMessage);
        auditLogRepository.save(entry);
    }

    public Page<AuditLog> findAll(Pageable pageable) {
        return auditLogRepository.findAllByOrderByExecutedAtDesc(pageable);
    }

    public Page<AuditLog> findByUser(String username, Pageable pageable) {
        return auditLogRepository.findByUsernameOrderByExecutedAtDesc(username, pageable);
    }
}
