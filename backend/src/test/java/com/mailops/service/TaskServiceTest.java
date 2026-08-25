package com.mailops.service;

import com.mailops.entity.Task;
import com.mailops.entity.TaskPriority;
import com.mailops.entity.TaskStatus;
import com.mailops.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void dispute_createsHighPriorityOpenTask() {
        when(taskRepository.findByEmailId(1L)).thenReturn(Optional.empty());
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task task = taskService.createDisputeTask(1L);

        assertEquals("Review invoice dispute", task.getTitle());
        assertEquals(TaskPriority.HIGH, task.getPriority());
        assertEquals(TaskStatus.OPEN, task.getStatus());
    }

    @Test
    void duplicateCall_returnsExistingTask() {
        Task existing = Task.builder().id(1L).emailId(1L).title("Review invoice dispute").build();
        when(taskRepository.findByEmailId(1L)).thenReturn(Optional.of(existing));

        Task result = taskService.createDisputeTask(1L);

        assertSame(existing, result);
        verify(taskRepository, never()).save(any());
    }
}
