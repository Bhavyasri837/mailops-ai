package com.mailops.controller;

import com.mailops.entity.Task;
import com.mailops.entity.TaskPriority;
import com.mailops.entity.TaskStatus;
import com.mailops.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskRepository taskRepository;

    @Test
    void listTasks_returnsCleanDtoShape() throws Exception {
        Task task = Task.builder()
                .id(1L)
                .emailId(7L)
                .title("Follow up: billing dispute")
                .description("Customer disputes charge on invoice INV-55.")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();
        when(taskRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(task));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Follow up: billing dispute"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].createdAt").exists());
    }

    @Test
    void listTasks_empty_returnsEmptyArray() throws Exception {
        when(taskRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
