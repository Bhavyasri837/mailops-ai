package com.mailops.repository;

import com.mailops.entity.ReplyDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReplyDraftRepository extends JpaRepository<ReplyDraft, Long> {

    Optional<ReplyDraft> findByEmailId(Long emailId);

    boolean existsByEmailId(Long emailId);

    List<ReplyDraft> findAllByOrderByCreatedAtDesc();
}
