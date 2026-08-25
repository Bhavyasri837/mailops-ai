package com.mailops.repository;

import com.mailops.entity.Action;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActionRepository extends JpaRepository<Action, Long> {

    List<Action> findByEmailIdOrderByCreatedAtDesc(Long emailId);

    Optional<Action> findTopByEmailIdOrderByCreatedAtDesc(Long emailId);
}
