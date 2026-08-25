package com.mailops.repository;

import com.mailops.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEmailIdOrderByCreatedAtAsc(Long emailId);

    List<AuditLog> findAllByOrderByCreatedAtDesc();
}
