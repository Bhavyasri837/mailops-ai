package com.mailops.repository;

import com.mailops.entity.Classification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassificationRepository extends JpaRepository<Classification, Long> {

    List<Classification> findByEmailIdOrderByCreatedAtDesc(Long emailId);

    Optional<Classification> findTopByEmailIdOrderByCreatedAtDesc(Long emailId);
}
