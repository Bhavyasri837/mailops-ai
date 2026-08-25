package com.mailops.service;

import com.mailops.entity.Task;
import com.mailops.entity.TaskPriority;
import com.mailops.entity.TaskStatus;
import com.mailops.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes the CREATE_FOLLOW_UP_TASK action for DISPUTE emails.
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    @Transactional
    public Task createDisputeTask(Long emailId) {
        return taskRepository.findByEmailId(emailId).orElseGet(() -> {
            Task task = Task.builder()
                    .emailId(emailId)
                    .title("Review invoice dispute")
                    .description("Review the dispute raised in the incoming email.")
                    .priority(TaskPriority.HIGH)
                    .status(TaskStatus.OPEN)
                    .build();
            try {
                return taskRepository.save(task);
            } catch (DataIntegrityViolationException e) {
                return taskRepository.findByEmailId(emailId).orElseThrow(() -> e);
            }
        });
    }
}
