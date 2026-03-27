package com.mysqlmanager.repository;

import com.mysqlmanager.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByUsernameOrderByExecutedAtDesc(String username, Pageable pageable);
    Page<AuditLog> findAllByOrderByExecutedAtDesc(Pageable pageable);
}
