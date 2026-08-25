package com.mailops.repository;

import com.mailops.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByEmailId(Long emailId);

    boolean existsByEmailId(Long emailId);

    List<Task> findAllByOrderByCreatedAtDesc();
}
