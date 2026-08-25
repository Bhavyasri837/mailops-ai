package com.mailops.repository;

import com.mailops.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByEmailId(Long emailId);

    boolean existsByEmailId(Long emailId);

    List<Invoice> findAllByOrderByCreatedAtDesc();
}
