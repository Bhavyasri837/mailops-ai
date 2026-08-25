package com.mailops.controller;

import com.mailops.dto.ReplyDraftDto;
import com.mailops.repository.ReplyDraftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reply-drafts")
@RequiredArgsConstructor
public class ReplyDraftController {

    private final ReplyDraftRepository replyDraftRepository;

    @GetMapping
    public List<ReplyDraftDto> list() {
        return replyDraftRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ReplyDraftDto::from)
                .toList();
    }
}
