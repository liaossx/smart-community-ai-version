package com.lsx.ai.customer.controller;

import com.lsx.ai.customer.dto.CustomerServiceAnswerResponse;
import com.lsx.ai.customer.dto.CustomerServiceAskRequest;
import com.lsx.ai.customer.service.CustomerServiceAssistant;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/community/customer-service")
public class CustomerServiceAiController {
    private final CustomerServiceAssistant assistant;

    public CustomerServiceAiController(CustomerServiceAssistant assistant) {
        this.assistant = assistant;
    }

    @PostMapping("/ask")
    public CustomerServiceAnswerResponse ask(@Valid @RequestBody CustomerServiceAskRequest request) {
        // RAG 客服入口：Controller 不做 AI 逻辑，只负责接收前端问题并交给 Assistant 编排链路。
        return assistant.answer(request);
    }
}
