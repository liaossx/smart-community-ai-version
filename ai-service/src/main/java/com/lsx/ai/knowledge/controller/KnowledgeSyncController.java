package com.lsx.ai.knowledge.controller;

import com.lsx.ai.knowledge.dto.KnowledgeSyncResponse;
import com.lsx.ai.knowledge.service.NoticeKnowledgeSyncService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/knowledge/sync")
public class KnowledgeSyncController {
    private final NoticeKnowledgeSyncService noticeKnowledgeSyncService;

    public KnowledgeSyncController(NoticeKnowledgeSyncService noticeKnowledgeSyncService) {
        this.noticeKnowledgeSyncService = noticeKnowledgeSyncService;
    }

    @PostMapping("/notices")
    public KnowledgeSyncResponse syncNotices() {
        return noticeKnowledgeSyncService.syncPublishedNotices();
    }
}
