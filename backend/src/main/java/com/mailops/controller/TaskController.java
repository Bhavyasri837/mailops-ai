package com.mailops.controller;

import com.mailops.dto.TaskDto;
import com.mailops.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskRepository taskRepository;

    @GetMapping
    public List<TaskDto> list() {
        return taskRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(TaskDto::from)
                .toList();
    }
}
