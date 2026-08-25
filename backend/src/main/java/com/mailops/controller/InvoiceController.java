package com.mailops.controller;

import com.mailops.dto.InvoiceDto;
import com.mailops.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;

    @GetMapping
    public List<InvoiceDto> list() {
        return invoiceRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(InvoiceDto::from)
                .toList();
    }
}
