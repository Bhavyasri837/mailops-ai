package com.mailops.repository;

import com.mailops.entity.Email;
import com.mailops.entity.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmailRepository extends JpaRepository<Email, Long> {

    Optional<Email> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    List<Email> findByStatus(EmailStatus status);

    @Query("""
            SELECT e FROM Email e
            WHERE (:status IS NULL OR e.status = :status)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(e.sender) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(e.subject) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(e.body) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY e.receivedAt DESC
            """)
    List<Email> search(@Param("status") EmailStatus status, @Param("search") String search);
}
